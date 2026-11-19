package com.example.ui.webview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.DinoyoConfig
import com.example.ui.WebViewCommand
import kotlinx.coroutines.flow.SharedFlow

private val EXTERNAL_APP_HOSTS = setOf(
  "wa.me",
  "api.whatsapp.com",
  "chat.whatsapp.com",
  "t.me",
  "telegram.me",
  "play.google.com",
  "maps.google.com",
)

private class WebViewHolder {
  var instance: WebView? = null
  var pendingFileCallback: ValueCallback<Array<Uri>>? = null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DinoyoWebView(
  initialUrl: String,
  commands: SharedFlow<WebViewCommand>,
  onProgressChanged: (Int) -> Unit,
  onPageStarted: (String) -> Unit,
  onPageFinished: (url: String, title: String?, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
  onHistoryUpdated: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
  onTitleReceived: (String?) -> Unit,
  onMainFrameError: (errorCode: Int, description: String) -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val currentOnProgressChanged by rememberUpdatedState(onProgressChanged)
  val currentOnPageStarted by rememberUpdatedState(onPageStarted)
  val currentOnPageFinished by rememberUpdatedState(onPageFinished)
  val currentOnHistoryUpdated by rememberUpdatedState(onHistoryUpdated)
  val currentOnTitleReceived by rememberUpdatedState(onTitleReceived)
  val currentOnMainFrameError by rememberUpdatedState(onMainFrameError)
  val currentOnShowMessage by rememberUpdatedState(onShowMessage)

  val savedWebViewState = rememberSaveable { Bundle() }
  val holder = remember { WebViewHolder() }

  val fileChooserLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
  ) { uris ->
    val callback = holder.pendingFileCallback
    holder.pendingFileCallback = null
    if (uris.isEmpty()) {
      callback?.onReceiveValue(null)
    } else {
      callback?.onReceiveValue(uris.toTypedArray())
    }
  }

  LaunchedEffect(commands) {
    commands.collect { command ->
      val view = holder.instance ?: return@collect
      when (command) {
        WebViewCommand.Reload -> view.reload()
        WebViewCommand.GoBack -> if (view.canGoBack()) view.goBack()
        is WebViewCommand.LoadUrl -> view.loadUrl(command.url)
      }
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      val view = holder.instance ?: return@LifecycleEventObserver
      when (event) {
        Lifecycle.Event.ON_RESUME -> view.onResume()
        Lifecycle.Event.ON_PAUSE -> view.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      holder.instance?.let { view ->
        view.saveState(savedWebViewState)
        view.stopLoading()
      }
      holder.pendingFileCallback?.onReceiveValue(null)
      holder.pendingFileCallback = null
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      WebView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setBackgroundColor(Color.parseColor("#F8F9FA"))
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = false
        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

        configureWebSettings(settings)

        val currentWebView = this
        CookieManager.getInstance().apply {
          setAcceptCookie(true)
          setAcceptThirdPartyCookies(currentWebView, true)
        }

        setDownloadListener(
          DownloadListener { url, _, _, _, _ ->
            if (!url.isNullOrBlank()) {
              openInExternalBrowser(ctx, url) {
                currentOnShowMessage(ctx.getString(R.string.msg_no_external_app))
              }
              currentOnShowMessage(ctx.getString(R.string.msg_download_started))
            }
          }
        )

        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
          ): Boolean {
            val uri = request?.url ?: return false
            return handleNavigationUri(
              context = ctx,
              uri = uri,
              onNoAppFound = {
                currentOnShowMessage(ctx.getString(R.string.msg_no_external_app))
              },
            )
          }

          override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            currentOnPageStarted(url ?: initialUrl)
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            currentOnPageFinished(
              url ?: view?.url.orEmpty(),
              view?.title,
              view?.canGoBack() == true,
              view?.canGoForward() == true,
            )
          }

          override fun doUpdateVisitedHistory(
            view: WebView?,
            url: String?,
            isReload: Boolean,
          ) {
            super.doUpdateVisitedHistory(view, url, isReload)
            currentOnHistoryUpdated(
              url ?: view?.url.orEmpty(),
              view?.canGoBack() == true,
              view?.canGoForward() == true,
            )
          }

          override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
          ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
              val code = error?.errorCode ?: -1
              if (code != ERROR_UNSUPPORTED_SCHEME && code != -1) {
                currentOnMainFrameError(
                  code,
                  error?.description?.toString().orEmpty(),
                )
              }
            }
          }

          override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?,
          ): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              view?.reload()
              return true
            }
            return super.onRenderProcessGone(view, detail)
          }
        }

        webChromeClient = object : WebChromeClient() {
          override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            currentOnProgressChanged(newProgress)
          }

          override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            currentOnTitleReceived(title)
          }

          override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
          ): Boolean {
            if (filePathCallback == null) return false
            holder.pendingFileCallback?.onReceiveValue(null)
            holder.pendingFileCallback = filePathCallback

            val mimeTypes = fileChooserParams?.acceptTypes
              ?.filter { it.isNotBlank() }
              ?.takeIf { it.isNotEmpty() }
              ?.toTypedArray()
              ?: arrayOf("*/*")

            return try {
              fileChooserLauncher.launch(mimeTypes)
              true
            } catch (_: ActivityNotFoundException) {
              holder.pendingFileCallback = null
              filePathCallback.onReceiveValue(null)
              currentOnShowMessage(ctx.getString(R.string.msg_no_external_app))
              false
            }
          }
        }

        if (savedWebViewState.isEmpty) {
          loadUrl(initialUrl)
        } else {
          restoreState(savedWebViewState)
        }

        holder.instance = this
      }
    },
    update = { view ->
      holder.instance = view
    },
  )
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebSettings(settings: WebSettings) {
  settings.javaScriptEnabled = true
  settings.domStorageEnabled = true
  settings.databaseEnabled = true
  settings.useWideViewPort = true
  settings.loadWithOverviewMode = true
  settings.setSupportZoom(true)
  settings.builtInZoomControls = true
  settings.displayZoomControls = false
  settings.setSupportMultipleWindows(false)
  settings.javaScriptCanOpenWindowsAutomatically = false
  settings.mediaPlaybackRequiresUserGesture = false
  settings.cacheMode = WebSettings.LOAD_DEFAULT
  settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
  settings.allowFileAccess = false
  settings.allowContentAccess = true
}

private fun handleNavigationUri(
  context: Context,
  uri: Uri,
  onNoAppFound: () -> Unit,
): Boolean {
  val scheme = uri.scheme?.lowercase().orEmpty()
  val host = uri.host?.lowercase().orEmpty()

  if (scheme == "http" || scheme == "https") {
    if (host in EXTERNAL_APP_HOSTS || host.endsWith(".wa.me")) {
      openInExternalBrowser(context, uri.toString(), onNoAppFound)
      return true
    }
    return false
  }

  if (scheme == "intent") {
    return try {
      val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        component = null
        selector = null
      }
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
      } else {
        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
        if (!fallbackUrl.isNullOrBlank()) {
          openInExternalBrowser(context, fallbackUrl, onNoAppFound)
        } else {
          onNoAppFound()
        }
      }
      true
    } catch (_: Exception) {
      onNoAppFound()
      true
    }
  }

  return try {
    val action = if (scheme == "tel") Intent.ACTION_DIAL else Intent.ACTION_VIEW
    val intent = Intent(action, uri)
    context.startActivity(intent)
    true
  } catch (_: ActivityNotFoundException) {
    onNoAppFound()
    true
  }
}

fun openInExternalBrowser(
  context: Context,
  url: String,
  onNoAppFound: () -> Unit = {},
) {
  try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    onNoAppFound()
  }
}
