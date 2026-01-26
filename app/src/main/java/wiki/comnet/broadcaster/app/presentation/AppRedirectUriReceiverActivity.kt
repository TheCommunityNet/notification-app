package wiki.comnet.broadcaster.app.presentation

import android.content.Intent
import android.os.Bundle
import net.openid.appauth.AuthorizationManagementActivity
import net.openid.appauth.RedirectUriReceiverActivity

class AppRedirectUriReceiverActivity : RedirectUriReceiverActivity() {
    override fun onCreate(savedInstanceBundle: Bundle?) {
        super.onCreate(savedInstanceBundle)

        val data = intent.data

        val url = data.toString()

        if (!url.startsWith("comnet-app://oauth/callback")) {
            startActivity(
                Intent(this, MainActivity::class.java)
            )
            finish()
            return
        }

        // while this does not appear to be achieving much, handling the redirect in this way
        // ensures that we can remove the browser tab from the back stack. See the documentation
        // on AuthorizationManagementActivity for more details.
        startActivity(
            AuthorizationManagementActivity.createResponseHandlingIntent(
                this, data
            )
        )
        finish()
    }
}