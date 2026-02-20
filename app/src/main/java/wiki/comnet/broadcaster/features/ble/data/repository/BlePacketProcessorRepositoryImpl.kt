package wiki.comnet.broadcaster.features.ble.data.repository

import wiki.comnet.broadcaster.features.logging.ComNetLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.features.ble.data.protocol.MessageType
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket
import wiki.comnet.broadcaster.features.ble.domain.repository.BleFragmentRepository
import wiki.comnet.broadcaster.features.ble.domain.repository.BlePacketProcessorRepository
import javax.inject.Inject

class BlePacketProcessorRepositoryImpl @Inject constructor(
    private val bleFragmentRepository: BleFragmentRepository,
) : BlePacketProcessorRepository {
    companion object {
        private val TAG = BlePacketProcessorRepository::class.java.simpleName
    }

    private val processorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val actors = mutableMapOf<String, kotlinx.coroutines.channels.SendChannel<BlePacket>>()


    @OptIn(ObsoleteCoroutinesApi::class)
    private fun getOrCreateActorForPeer(peerID: String) = processorScope.actor<BlePacket>(
        capacity = Channel.UNLIMITED
    ) {
        try {
            for (packet in channel) {
                ComNetLog.d(TAG, "📦 Processing packet type ${packet.type} (serialized)")
                handleReceivedPacket(packet)
                ComNetLog.d(TAG, "Completed packet type ${packet.type} ")
            }
        } finally {
            ComNetLog.d(TAG, "🎭 Packet actor for terminated")
        }
    }

    private val _message = MutableStateFlow<BlePacket?>(null)

    override val message = _message.asStateFlow()

    override fun processPacket(packet: BlePacket) {
        val id = String(packet.id)

        val actor = actors.getOrPut(id) { getOrCreateActorForPeer(id) }

        processorScope.launch {
            try {
                actor.send(packet)
            } catch (e: Exception) {
                ComNetLog.w(TAG, "Failed to send packet to actor for : ${e.message}")
                // Fallback to direct processing if actor fails
                handleReceivedPacket(packet)
            }
        }
    }

    private fun handleReceivedPacket(packet: BlePacket) {
        val messageType = MessageType.fromValue(packet.type)

        when (messageType) {
            MessageType.MESSAGE -> {
                handleMessage(packet)
            }

            MessageType.NOTIFICATION_TRACKING -> {
                handleMessage(packet)
            }

            MessageType.FRAGMENT -> {
                handleFragment(packet)
            }

            else -> {}
        }
    }

    private fun handleMessage(packet: BlePacket) {
        ComNetLog.d(TAG, "Processing message")
        _message.value = packet
    }

    private fun handleFragment(packet: BlePacket) {
        ComNetLog.d(TAG, "Processing fragment")

        val reassembledPacket = bleFragmentRepository.handleFragment(packet)
        if (reassembledPacket != null) {
            ComNetLog.d(TAG, "Fragment reassembled, processing complete message")
            processPacket(reassembledPacket)
        }
    }
}