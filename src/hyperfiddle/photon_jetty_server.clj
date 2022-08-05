(ns hyperfiddle.photon-jetty-server
  (:require [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.adapter.jetty9 :as ring]
            [hyperfiddle.photon-jetty-adapter :as adapter]
            [hyperfiddle.logger :as log]
            [ring.middleware.basic-authentication :as auth]
            [ring.middleware.cookies :as cookies]
            [ring.util.response :as res])
  (:import [java.io IOException]
           [java.net BindException]))

(defn authenticate [username password] username) ; demo (accept-all) authentication

(defn wrap-demo-authentication [next-handler]
  ;; Authentication demo, accept any user/password, stores the username in a cookie
  (-> (fn [ring-req]
        (let [res (next-handler ring-req)]
          (if-let [username (:basic-authentication ring-req)]
            (res/set-cookie res "username" username {:http-only true})
            res)))
    (cookies/wrap-cookies)
    (auth/wrap-basic-authentication authenticate)))

(defn wrap-default-page [next-handler]
  (fn [ring-req]
    (case (:uri ring-req)
      ("" "/") (next-handler (assoc ring-req :uri "/index.html"))
      ("/auth") (let [response  ((wrap-demo-authentication next-handler) ring-req)]
                  (if (= 401 (:status response)) ; authenticated?
                    response                     ; send response to trigger auth prompt
                    (-> (res/status response 302) ; redirect
                        (res/header "Location" "/"))))
      (next-handler ring-req))))

#_
(defn wrap-photon [next-handler]        ; Jetty 10 allows such handler
  (fn [ring-request]
    (if (ring/ws-upgrade-request? ring-request)
      (ring/ws-upgrade-response (adapter/photon-ws-adapter adapter/photon-ws-message-handler))
      (next-handler ring-request))))

(defn default-handler [ring-request]
  {:status 404
   :body "not found"})

(defn start-server! [config]
  (try
    (ring/run-jetty (-> default-handler
                      (wrap-file (or (:resources-path config) "resources/public"))
                      (wrap-content-type)
                      (wrap-default-page)
                      #_(wrap-photon))
      ;; Jetty 9 forces us to declare WS paths out of a ring handler.
      (merge {:port       8080
              :join?      false
              :websockets {"/" (fn [ring-req]
                                 (adapter/photon-ws-adapter
                                   (partial adapter/photon-ws-message-handler
                                     (-> ring-req
                                       (auth/basic-authentication-request authenticate)
                                       (cookies/cookies-request)))))}}
        config))
    (catch IOException err
      (if (instance? BindException (ex-cause err))
        (do (log/warn "Port" (:port config) "was not available, retrying with" (inc (:port config)))
          (start-server! (update config :port inc)))
        (throw err)))))
