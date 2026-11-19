package com.example.ui.webview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

  val fileChooserLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
  ) { uris ->
    val callback = pendingFileCallback
    pendingFileCallback = null
    if (uris.isEmpty()) {
      callback?.onReceiveValue(null)
    } else {
      callback?.onReceiveValue(uris.toTypedArray())
    }
  }

  // Handle commands from ViewModel
  LaunchedEffect(commands, webViewRef) {
    val view = webViewRef ?: return@LaunchedEffect
    commands.collect { command ->
      when (command) {
        WebViewCommand.Reload -> view.reload()
        WebViewCommand.GoBack -> if (view.canGoBack()) view.goBack()
        is WebViewCommand.LoadUrl -> view.loadUrl(command.url)
      }
    }
  }

  // Pause/resume WebView with lifecycle and save state on dispose
  DisposableEffect(lifecycleOwner, webViewRef) {
    val observer = LifecycleEventObserver { _, event ->
      val view = webViewRef ?: return@LifecycleEventObserver
      when (event) {
        Lifecycle.Event.ON_RESUME -> view.onResume()
        Lifecycle.Event.ON_PAUSE -> view.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      webViewRef?.let { view ->
        view.saveState(savedWebViewState)
        view.stopLoading()
      }
      pendingFileCallback?.onReceiveValue(null)
      pendingFileCallback = null
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
              // Ignore user/navigation-aborted cancellations
              if (code != ERROR_UNSUPPORTED_SCHEME && code != -1) {
                currentOnMainFrameError(
                  code,
                  error?.description?.toString().orEmpty(),
                )
              }
            }
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
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = filePathCallback

            val mimeTypes = fileChooserParams?.acceptTypes
              ?.filter { it.isNotBlank() }
              ?.takeIf { it.isNotEmpty() }
              ?.toTypedArray()
              ?: arrayOf("*/*")

            return try {
              fileChooserLauncher.launch(mimeTypes)
              true
            } catch (_: ActivityNotFoundException) {
              pendingFileCallback = null
              filePathCallback.onReceiveValue(null)
              currentOnShowMessage(ctx.getString(R.string.msg_no_external_app))
              false
            }
          }

          override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?,
          ): Boolean {
            val mainView = view ?: return false
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false

            val tempWebView = WebView(ctx).apply {
              webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                  temp: WebView?,
                  request: WebResourceRequest?,
                ): Boolean {
                  val targetUri = request?.url
                  if (targetUri != null) {
                    val handledExternally = handleNavigationUri(
                      context = ctx,
                      uri = targetUri,
                      onNoAppFound = {
                        currentOnShowMessage(ctx.getString(R.string.msg_no_external_app))
                      },
                    )
                    if (!handledExternally) {
                      mainView.loadUrl(targetUri.toString())
                    }
                  }
                  temp?.destroy()
                  return true
                }
              }
            }
            transport.webView = tempWebView
            resultMsg.sendToTarget()
            return true
          }
        }

        if (savedWebViewState.isEmpty) {
          loadUrl(initialUrl)
        } else {
          restoreState(savedWebViewState)
        }

        webViewRef = this
      }
    },
    update = { view ->
      if (webViewRef !== view) {
        webViewRef = view
      }
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
  settings.setSupportMultipleWindows(true)
  settings.javaScriptCanOpenWindowsAutomatically = true
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
    // Allow Dinoyo and standard HTTPS web flows (including OAuth/CDN) inside WebView,
    // while routing known messaging/store deep links to native apps.
    if (host in EXTERNAL_APP_HOSTS || host.endsWith(".wa.me")) {
      openInExternalBrowser(context, uri.toString(), onNoAppFound)
      return true
    }
    if (scheme == "http" && host == DinoyoConfig.HOME_HOST) {
      return false
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

  // tel:, mailto:, smsto:, geo:, whatsapp:, etc.
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
