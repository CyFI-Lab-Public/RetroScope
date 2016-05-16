/*
 * Copyright 2010, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// This class gathers state related to a single user profile.
// On Android, we only use this for AutoFill so methods are mostly
// just stubs.

#ifndef ANDROID_AUTOFILL_PROFILE_H_
#define ANDROID_AUTOFILL_PROFILE_H_

#include "android/autofill/android_url_request_context_getter.h"
#include "base/basictypes.h"
#include "base/file_path.h"
#include "base/memory/ref_counted.h"
#include "base/scoped_ptr.h"
#include "base/timer.h"
#include "chrome/browser/prefs/pref_service.h"
#include "chrome/browser/profiles/profile.h"

namespace base {
class Time;
}

namespace history {
class TopSites;
}

namespace fileapi {
class FileSystemContext;
}

namespace net {
class TransportSecurityState;
class SSLConfigService;
}

namespace policy {
class ProfilePolicyContext;
}

namespace prerender {
class PrerenderManager;
}

namespace webkit_database {
class DatabaseTracker;
}

class AutocompleteClassifier;
class BackgroundContentsService;
class BookmarkModel;
class BrowserSignin;
class BrowserThemeProvider;
class ChromeURLRequestContextGetter;
class DesktopNotificationService;
class DownloadManager;
class Extension;
class ExtensionDevToolsManager;
class ExtensionIOEventRouter;
class ExtensionProcessManager;
class ExtensionMessageService;
class ExtensionsService;
class FaviconService;
class FindBarState;
class GeolocationContentSettingsMap;
class GeolocationPermissionContext;
class HistoryService;
class HostContentSettingsMap;
class HostZoomMap;
class NavigationController;
class NTPResourceCache;
class PasswordStore;
class PersonalDataManager;
class PinnedTabService;
class PrefProxyConfigTracker;
class PromoCounter;
class ProfileSyncService;
class ProfileSyncFactory;
class SessionService;
class SpellCheckHost;
class SSLConfigServiceManager;
class SSLHostState;
class TransportSecurityPersister;
class SQLitePersistentCookieStore;
class TabRestoreService;
class TemplateURLFetcher;
class TemplateURLModel;
class ThemeProvider;
class TokenService;
class URLRequestContextGetter;
class UserScriptMaster;
class UserStyleSheetWatcher;
class VisitedLinkMaster;
class VisitedLinkEventListener;
class WebDataService;
class WebKitContext;
class WebResourceService;
class CloudPrintProxyService;

typedef intptr_t ProfileId;

// The android profile implementation.
class ProfileImplAndroid : public Profile {
 public:
  virtual ~ProfileImplAndroid();

  void SetRequestContext(net::URLRequestContextGetter* context) { url_request_context_getter_ = context; }

  // Profile implementation.
  virtual Profile* GetOriginalProfile();
  virtual PersonalDataManager* GetPersonalDataManager();
  virtual PrefService* GetPrefs();
  virtual FilePath GetPath() { return path_; }
  virtual net::URLRequestContextGetter* GetRequestContext();

  // Functions from Profile that we don't need on Android for AutoFill.
  virtual ProfileId GetRuntimeId() { NOTREACHED(); return 0; }
  virtual bool IsOffTheRecord() { NOTREACHED(); return false; }
  virtual Profile* GetOffTheRecordProfile() { NOTREACHED(); return NULL; }
  virtual void DestroyOffTheRecordProfile() { NOTREACHED(); }
  virtual bool HasOffTheRecordProfile() { NOTREACHED(); return false; }
  virtual ChromeAppCacheService* GetAppCacheService() { NOTREACHED(); return NULL; }
  virtual webkit_database::DatabaseTracker* GetDatabaseTracker() { NOTREACHED(); return NULL; }
  virtual history::TopSites* GetTopSites() { NOTREACHED(); return NULL; }
  virtual VisitedLinkMaster* GetVisitedLinkMaster() { NOTREACHED(); return NULL; }
  virtual UserScriptMaster* GetUserScriptMaster() { NOTREACHED(); return NULL; }
  virtual SSLHostState* GetSSLHostState() { NOTREACHED(); return NULL; }
  virtual net::TransportSecurityState* GetTransportSecurityState() { NOTREACHED(); return NULL; }
  virtual ExtensionsService* GetExtensionsService() { NOTREACHED(); return NULL; }
  virtual ExtensionDevToolsManager* GetExtensionDevToolsManager() { NOTREACHED(); return NULL; }
  virtual ExtensionProcessManager* GetExtensionProcessManager() { NOTREACHED(); return NULL; }
  virtual ExtensionMessageService* GetExtensionMessageService() { NOTREACHED(); return NULL; }
  virtual ExtensionEventRouter* GetExtensionEventRouter() { NOTREACHED(); return NULL; }
  virtual ExtensionIOEventRouter* GetExtensionIOEventRouter() { NOTREACHED(); return NULL; };
  virtual ExtensionService* GetExtensionService() { NOTREACHED(); return NULL; }
  virtual ExtensionSpecialStoragePolicy* GetExtensionSpecialStoragePolicy() { NOTREACHED(); return NULL; }
  virtual FaviconService* GetFaviconService(ServiceAccessType sat) { NOTREACHED(); return NULL; }
  virtual HistoryService* GetHistoryService(ServiceAccessType sat) { NOTREACHED(); return NULL; }
  virtual HistoryService* GetHistoryServiceWithoutCreating() { NOTREACHED(); return NULL; }
  virtual AutocompleteClassifier* GetAutocompleteClassifier() { NOTREACHED(); return NULL; }
  virtual WebDataService* GetWebDataService(ServiceAccessType sat) { NOTREACHED(); return NULL; }
  virtual WebDataService* GetWebDataServiceWithoutCreating() { NOTREACHED(); return NULL; }
  virtual PasswordStore* GetPasswordStore(ServiceAccessType sat) { NOTREACHED(); return NULL; }
  virtual ProtocolHandlerRegistry* GetProtocolHandlerRegistry() { NOTREACHED(); return NULL; }
  virtual PrefService* GetOffTheRecordPrefs() { NOTREACHED(); return NULL; }
  virtual policy::ProfilePolicyConnector* GetPolicyConnector() { NOTREACHED(); return NULL; }
  virtual TemplateURLModel* GetTemplateURLModel() { NOTREACHED(); return NULL; }
  virtual TemplateURLFetcher* GetTemplateURLFetcher() { NOTREACHED(); return NULL; }
  virtual DownloadManager* GetDownloadManager() { NOTREACHED(); return NULL; }
  virtual fileapi::FileSystemContext* GetFileSystemContext() { NOTREACHED(); return NULL; }
  virtual void InitPromoResources() { NOTREACHED(); }
  virtual void InitRegisteredProtocolHandlers() { NOTREACHED(); }
  virtual void InitThemes() { NOTREACHED(); }
  virtual void SetTheme(const Extension* extension) { NOTREACHED(); }
  virtual void SetNativeTheme() { NOTREACHED(); }
  virtual void ClearTheme() { NOTREACHED(); }
  virtual const Extension* GetTheme() { NOTREACHED(); return NULL; }
  virtual BrowserThemeProvider* GetThemeProvider()  { NOTREACHED(); return NULL; }
  virtual bool HasCreatedDownloadManager() const { NOTREACHED(); return false; }
  virtual net::URLRequestContextGetter* GetRequestContextForMedia()  { NOTREACHED(); return NULL; }
  virtual net::URLRequestContextGetter* GetRequestContextForExtensions()  { NOTREACHED(); return NULL; }
  virtual void RegisterExtensionWithRequestContexts(const Extension* extension) { NOTREACHED(); }
  virtual void UnregisterExtensionWithRequestContexts(const Extension* extension) { NOTREACHED(); }
  virtual net::SSLConfigService* GetSSLConfigService()  { NOTREACHED(); return NULL; }
  virtual HostContentSettingsMap* GetHostContentSettingsMap()  { NOTREACHED(); return NULL; }
  virtual HostZoomMap* GetHostZoomMap()  { NOTREACHED(); return NULL; }
  virtual GeolocationContentSettingsMap* GetGeolocationContentSettingsMap()  { NOTREACHED(); return NULL; }
  virtual GeolocationPermissionContext* GetGeolocationPermissionContext()  { NOTREACHED(); return NULL; }
  virtual UserStyleSheetWatcher* GetUserStyleSheetWatcher()  { NOTREACHED(); return NULL; }
  virtual FindBarState* GetFindBarState()  { NOTREACHED(); return NULL; }
  virtual SessionService* GetSessionService()  { NOTREACHED(); return NULL; }
  virtual void ShutdownSessionService() { NOTREACHED(); }
  virtual bool HasSessionService() const { NOTREACHED(); return false; }
  virtual bool DidLastSessionExitCleanly() { NOTREACHED(); return true; }
  virtual BookmarkModel* GetBookmarkModel()  { NOTREACHED(); return NULL; }
  virtual bool IsSameProfile(Profile* profile) { NOTREACHED(); return false; }
  virtual base::Time GetStartTime() const  { NOTREACHED(); return base::Time(); }
  virtual TabRestoreService* GetTabRestoreService()  { NOTREACHED(); return NULL; }
  virtual void ResetTabRestoreService() { NOTREACHED(); }
  virtual SpellCheckHost* GetSpellCheckHost()  { NOTREACHED(); return NULL; }
  virtual void ReinitializeSpellCheckHost(bool force) { NOTREACHED(); }
  virtual WebKitContext* GetWebKitContext()  { NOTREACHED(); return NULL; }
  virtual DesktopNotificationService* GetDesktopNotificationService() { NOTREACHED(); return NULL; }
  virtual BackgroundContentsService* GetBackgroundContentsService() const { NOTREACHED(); return NULL; }
  virtual StatusTray* GetStatusTray() { NOTREACHED(); return NULL; }
  virtual void MarkAsCleanShutdown() { NOTREACHED(); }
  virtual void InitExtensions(bool extensions_enabled) { NOTREACHED(); }
  virtual void InitWebResources() { NOTREACHED(); }
  virtual NTPResourceCache* GetNTPResourceCache()  { NOTREACHED(); return NULL; }
  virtual FilePath last_selected_directory() { NOTREACHED(); return FilePath(""); }
  virtual void set_last_selected_directory(const FilePath& path) { NOTREACHED(); }
  virtual ChromeBlobStorageContext* GetBlobStorageContext() { NOTREACHED(); return NULL; }
  virtual ExtensionInfoMap* GetExtensionInfoMap() { NOTREACHED(); return NULL; }
  virtual PromoCounter* GetInstantPromoCounter() { NOTREACHED(); return NULL; }
  virtual ProfileSyncService* GetProfileSyncService()  { NOTREACHED();  return NULL; }
  virtual ProfileSyncService* GetProfileSyncService(const std::string&) { NOTREACHED(); return NULL; }
  virtual TokenService* GetTokenService()  { NOTREACHED(); return NULL; }
  void InitSyncService() { NOTREACHED(); }
  virtual CloudPrintProxyService* GetCloudPrintProxyService()  { NOTREACHED(); return NULL; }
  void InitCloudPrintProxyService() { NOTREACHED(); }

  virtual history::TopSites* GetTopSitesWithoutCreating() { NOTREACHED(); return NULL; }
  virtual BrowserSignin* GetBrowserSignin() { NOTREACHED(); return NULL; }
  virtual bool HasProfileSyncService() const { NOTREACHED(); return false; }

  virtual policy::ProfilePolicyContext* GetPolicyContext() { NOTREACHED(); return NULL; }
  virtual ChromeURLDataManager* GetChromeURLDataManager() { NOTREACHED(); return NULL; }
  virtual PrefProxyConfigTracker* GetProxyConfigTracker() { NOTREACHED(); return NULL; }
  virtual prerender::PrerenderManager* GetPrerenderManager() { NOTREACHED(); return NULL; }
  virtual net::URLRequestContextGetter* GetRequestContextForPossibleApp(
      const Extension* installed_app) { NOTREACHED(); return NULL; }
  virtual net::URLRequestContextGetter* GetRequestContextForIsolatedApp(
      const std::string& app_id) { NOTREACHED(); return NULL; }

 private:
  friend class Profile;

  explicit ProfileImplAndroid(const FilePath& path);

  void CreateWebDataService() { NOTREACHED(); }
  FilePath GetPrefFilePath() { return path_; }

  void CreatePasswordStore() { NOTREACHED(); }

  void StopCreateSessionServiceTimer() { NOTREACHED(); }

  void EnsureRequestContextCreated() {
    GetRequestContext();
  }

  void EnsureSessionServiceCreated() {
    GetSessionService();
  }

  FilePath path_;
  scoped_ptr<PrefService> preferences_;
  scoped_refptr<PersonalDataManager> personal_data_;
  scoped_refptr<net::URLRequestContextGetter> url_request_context_getter_;

  DISALLOW_COPY_AND_ASSIGN(ProfileImplAndroid);
};

#endif  // CHROME_BROWSER_PROFILE_H_
