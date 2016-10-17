module services {

    angular.module('website-services', [
        'ngRoute',
        'ngResource',
        'ngCookies',
        'ngStorage',
        'website-browser',
        'tribe-alerts'
    ])

        .factory('tribeFilterService', ['$location',
            function ($location) {
                return {
                    filterByCategory: function (app, category) {
                        $location.search({
                            a: app,
                            c: category
                        });
                        $location.path('/');
                    },
                    filterByRole: function (app, role) {
                        $location.search({
                            a: app,
                            r: role
                        });
                        $location.path('/');
                    },
                    filterByTag: function (app, tag) {
                        $location.search({
                            a: app,
                            t: tag
                        });
                        $location.path('/');
                    }
                };
            }
        ])

        .factory('tribeAuthorizationService', ['$http', '$localStorage', 'tribeHeaderProviderSelector',
            function ($http, $localStorage, tribeHeaderProviderSelector) {
                return {
                    getOauth2Status: function() {
                        return $http.get('api/security/oauth2/status');
                    },
                    setCredentials: function (username, providerState) {
                        $localStorage.tribe.security = providerState;
                        $localStorage.tribe.username = username == null ? "Guest" : username;
                    },
                    getCredentials: function () {
                        return $localStorage.tribe.username;
                    },
                    restoreSession: function () {
                        var providerState = $localStorage.tribe.security;
                        if (providerState != null) {
                            var provider = tribeHeaderProviderSelector
                                .select($localStorage.tribe.security.type);
                            provider
                                .fromState($localStorage.tribe.security);
                            return provider;
                        }
                    },
                    initContext: function () {
                        if ($localStorage.tribe == null) {
                            $localStorage.tribe = {};
                            return undefined;
                        } else {
                            return this.restoreSession();
                        }
                    },
                    isConnected: function () {
                        return $localStorage.tribe
                            && $localStorage.tribe.security;
                    },
                    clearCredentials: function () {
                        try {
                            document.execCommand('ClearAuthenticationCache');
                        } catch (e) {
                            // chrome does not support it
                        }
                        $localStorage.tribe = {};
                    }
                };
            }
        ])

        .factory('tribeServerService', ['$resource', '$http', 'tribeErrorHandlerService',
            function ($resource, $http, tribeErrorHandlerService) {
                var resource = $resource('api/server/info', null, {
                    info: {method: 'GET', params: {}, isArray: false}
                });
                return {
                    getInfo: function () {
                        return {
                            then: function (successCallback, errorCallback) {
                                resource.info({},
                                    successCallback,
                                    tribeErrorHandlerService.ensureErrorHandler(errorCallback)
                                );
                            }
                        };
                    }
                };
            }
        ])

        .run(function () {
            // placeholder
        });
}
