package sk.backbone.parent.repositories.server.client.exceptions

import com.android.volley.VolleyError

class GatewayTimeoutException(volleyError: VolleyError) : ParentHttpException(volleyError) {

}
