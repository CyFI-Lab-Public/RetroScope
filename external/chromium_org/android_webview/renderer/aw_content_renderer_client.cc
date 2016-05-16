// Copyright 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android_webview/renderer/aw_content_renderer_client.h"

#include "android_webview/common/aw_resource.h"
#include "android_webview/common/render_view_messages.h"
#include "android_webview/common/url_constants.h"
#include "android_webview/renderer/aw_render_view_ext.h"
// START: Printing fork b/10190508
#include "android_webview/renderer/print_web_view_helper.h"
// END: Printing fork b/10190508
#include "base/command_line.h"
#include "base/message_loop/message_loop.h"
#include "base/strings/utf_string_conversions.h"
#include "components/autofill/content/renderer/autofill_agent.h"
#include "components/autofill/content/renderer/password_autofill_agent.h"
#include "components/visitedlink/renderer/visitedlink_slave.h"
#include "content/public/common/url_constants.h"
#include "content/public/renderer/document_state.h"
#include "content/public/renderer/navigation_state.h"
#include "content/public/renderer/render_thread.h"
#include "content/public/renderer/render_view.h"
#include "net/base/net_errors.h"
#include "third_party/WebKit/public/platform/WebString.h"
#include "third_party/WebKit/public/web/WebFrame.h"
#include "third_party/WebKit/public/web/WebHistoryItem.h"
#include "third_party/WebKit/public/web/WebView.h"
#include "third_party/WebKit/public/platform/WebURL.h"
#include "third_party/WebKit/public/platform/WebURLError.h"
#include "third_party/WebKit/public/platform/WebURLRequest.h"
#include "third_party/WebKit/public/web/WebNavigationType.h"
#include "third_party/WebKit/public/web/WebSecurityPolicy.h"
#include "url/gurl.h"

using content::RenderThread;

namespace android_webview {

AwContentRendererClient::AwContentRendererClient() {
}

AwContentRendererClient::~AwContentRendererClient() {
}

void AwContentRendererClient::RenderThreadStarted() {
  WebKit::WebString content_scheme(
      ASCIIToUTF16(android_webview::kContentScheme));
  WebKit::WebSecurityPolicy::registerURLSchemeAsLocal(content_scheme);

  RenderThread* thread = content::RenderThread::Get();

  aw_render_process_observer_.reset(new AwRenderProcessObserver);
  thread->AddObserver(aw_render_process_observer_.get());

  visited_link_slave_.reset(new visitedlink::VisitedLinkSlave);
  thread->AddObserver(visited_link_slave_.get());
}

bool AwContentRendererClient::HandleNavigation(
    content::RenderView* view,
    content::DocumentState* document_state,
    int opener_id,
    WebKit::WebFrame* frame,
    const WebKit::WebURLRequest& request,
    WebKit::WebNavigationType type,
    WebKit::WebNavigationPolicy default_policy,
    bool is_redirect) {

  // Only GETs can be overridden.
  if (!request.httpMethod().equals("GET"))
    return false;

  // Any navigation from loadUrl, and goBack/Forward are considered application-
  // initiated and hence will not yield a shouldOverrideUrlLoading() callback.
  // Webview classic does not consider reload application-initiated so we
  // continue the same behavior.
  bool application_initiated =
      !document_state->navigation_state()->is_content_initiated()
      || type == WebKit::WebNavigationTypeBackForward;

  // Don't offer application-initiated navigations unless it's a redirect.
  if (application_initiated && !is_redirect)
    return false;

  const GURL& gurl = request.url();
  // We allow intercepting navigations within subframes, but only if the
  // scheme other than http or https. This is because the embedder
  // can't distinguish main frame and subframe callbacks (which could lead
  // to broken content if the embedder decides to not ignore the main frame
  // navigation, but ignores the subframe navigation).
  // The reason this is supported at all is that certain JavaScript-based
  // frameworks use iframe navigation as a form of communication with the
  // embedder.
  // HandleNavigation receives about:blank navigations, (for example for
  // empty iframes), however webview classic does not pass these to the
  // app using shouldoverrideurlloading, so we filter them out here. We
  // do not filter out top level about:blanks since they are allowed in
  // Webview classic. The about:blank behavior for child frames is covered
  // by testCalledOnJavaScriptLocationImmediateAssignRedirect().
  // TODO(sgurun) Need to write a test for allowing about:blank for top
  // navigations.
  if (frame->parent() && (gurl.SchemeIs(chrome::kHttpScheme) ||
                          gurl.SchemeIs(chrome::kHttpsScheme) ||
                          gurl.SchemeIs(chrome::kAboutScheme)))
    return false;

  // use NavigationInterception throttle to handle the call as that can
  // be deferred until after the java side has been constructed.
  if (opener_id != MSG_ROUTING_NONE)
    return false;

  bool ignore_navigation = false;
  base::string16 url =  request.url().string();

  int routing_id = view->GetRoutingID();
  RenderThread::Get()->Send(new AwViewHostMsg_ShouldOverrideUrlLoading(
      routing_id, url, &ignore_navigation));
  return ignore_navigation;
}

bool AwContentRendererClient::ShouldAbortNavigationAfterUrlResolve(
    content::RenderView* view,
    const GURL& base,
    const base::string16& fragment,
    const GURL& result) {
  if (!CommandLine::ForCurrentProcess()->HasSwitch(
        "enable-webview-classic-workarounds")) {
    return false;
  }
  if (!base.is_valid())
    return false;

  if (result.is_valid()) {
    // Workaround for http://b/11118423 -- some apps put "|" into an <a href>
    // and expect it to resolve.
    if (fragment.empty() || fragment[0] != '|')
      return false;
  }
  int routing_id = view->GetRoutingID();
  bool ignore_navigation = false;
  RenderThread::Get()->Send(new AwViewHostMsg_ShouldOverrideUrlLoading(
      routing_id, fragment, &ignore_navigation));
  if (ignore_navigation) {
    LOG(WARNING) << "Invalid URL resolve in navigation:\n"
        "   relative link: " << fragment << "\n"
        "   against base: " << base.spec() << "]\n"
        "^^^ THIS WILL BREAK IN FUTURE ANDROID WEBVIEW VERSIONS.";
  }
  return ignore_navigation;
}

void AwContentRendererClient::RenderViewCreated(
    content::RenderView* render_view) {
  AwRenderViewExt::RenderViewCreated(render_view);

// START: Printing fork b/10190508
  printing::PrintWebViewHelper* print_helper =
      new printing::PrintWebViewHelper(render_view);
  print_helper->SetScriptedPrintBlocked(true);
// END: Printing fork b/10190508
  // TODO(sgurun) do not create a password autofill agent (change
  // autofill agent to store a weakptr).
  autofill::PasswordAutofillAgent* password_autofill_agent =
      new autofill::PasswordAutofillAgent(render_view);
  new autofill::AutofillAgent(render_view, password_autofill_agent);
}

std::string AwContentRendererClient::GetDefaultEncoding() {
  return AwResource::GetDefaultTextEncoding();
}

bool AwContentRendererClient::HasErrorPage(int http_status_code,
                          std::string* error_domain) {
  return http_status_code >= 400;
}

void AwContentRendererClient::GetNavigationErrorStrings(
    WebKit::WebFrame* /* frame */,
    const WebKit::WebURLRequest& failed_request,
    const WebKit::WebURLError& error,
    std::string* error_html,
    string16* error_description) {
  if (error_html) {
    GURL error_url(failed_request.url());
    std::string err = UTF16ToUTF8(error.localizedDescription);
    std::string contents;
    if (err.empty()) {
      contents = AwResource::GetNoDomainPageContent();
    } else {
      contents = AwResource::GetLoadErrorPageContent();
      ReplaceSubstringsAfterOffset(&contents, 0, "%e", err);
    }

    ReplaceSubstringsAfterOffset(&contents, 0, "%s",
                                 error_url.possibly_invalid_spec());
    *error_html = contents;
  }
  if (error_description) {
    if (error.localizedDescription.isEmpty())
      *error_description = ASCIIToUTF16(net::ErrorToString(error.reason));
    else
      *error_description = error.localizedDescription;
  }
}

unsigned long long AwContentRendererClient::VisitedLinkHash(
    const char* canonical_url,
    size_t length) {
  return visited_link_slave_->ComputeURLFingerprint(canonical_url, length);
}

bool AwContentRendererClient::IsLinkVisited(unsigned long long link_hash) {
  return visited_link_slave_->IsVisited(link_hash);
}

}  // namespace android_webview
