package sk.backbone.parent.repositories.server.client.exceptions

import com.android.volley.VolleyError

class LoopDetectedException(volleyError: VolleyError) : ParentHttpException(volleyError) {

}
