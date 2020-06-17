package sk.backbone.android.shared.repositories.server.client

import android.net.Uri
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import org.json.JSONException
import org.json.JSONObject
import sk.backbone.android.shared.repositories.server.client.exceptions.BaseHttpException
import sk.backbone.android.shared.repositories.server.client.exceptions.IExceptionsErrorParser
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class BaseHttpRequest<T>(
    continuation: Continuation<T?>,
    requestMethod: Int,
    uri: Uri,
    body: String,
    parseSuccessResponse: (JSONObject?) -> T?,
    errorParser: IExceptionsErrorParser
): JsonRequest<JSONObject>(requestMethod, uri.toString(), body, onSuccess(continuation, parseSuccessResponse), onError(errorParser, continuation)){
    init {
        retryPolicy = DefaultRetryPolicy(
            60000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject>? {
        return try {
            val jsonString = String(response.data, Charset.forName(HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET)))

            if (jsonString.isEmpty() && response.statusCode == 204) {
                return Response.success(null, HttpHeaderParser.parseCacheHeaders(response))
            }

            Response.success(JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response))
        }
        catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        }
        catch (je: JSONException) {
            Response.error(ParseError(je))
        }
    }

    companion object {
        private fun <T>onSuccess(continuation: Continuation<T?>, parseSuccessResponse: (JSONObject?) -> T?): Response.Listener<JSONObject?>{
            return Response.Listener {
                Log.i("HttpResponseBody", it.toString())
                val response = parseSuccessResponse(it)
                continuation.resume(response)
            }
        }

        private fun onError(errorParser: IExceptionsErrorParser, continuation: Continuation<*>): Response.ErrorListener{
            return Response.ErrorListener {
                Log.i("HttpResponseBody", BaseHttpException.getResponseBody(it).toString())
                val exception = BaseHttpException.parseException(it, errorParser)
                continuation.resumeWithException(exception)
            }
        }
    }
}