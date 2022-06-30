(ns wip.ui-components
  (:require [hyperfiddle.photon :as p]
            [hyperfiddle.photon-dom :as dom]
            [hyperfiddle.photon-ui :as ui]
            [clojure.string :as str])
  (:import (hyperfiddle.photon Pending))
  #?(:cljs (:require-macros wip.ui-components)))        ; forces shadow hot reload to also reload JVM at the same time

(defn query-names [needle]
  (->> [{:id 1, :text "alice"},
        {:id 2, :text "bob"}
        {:id 3, :text "charlie"}]
       (filter #(str/includes? (:text %) needle))))

(defn run-long-task! []
  (prn "Running long task")
  #?(:clj (Thread/sleep 1000))
  :result)

(p/defn App []
  (dom/hiccup
   [:div
    [:h1 "UI Components"]
    [:hr]
    [:h2 "Button"]

    (when-let [event (ui/button {:on-click (p/fn [event] (ui/event :click true))}
                       (dom/text "log to console"))]
      (prn "clicked! " event))

    [:hr]
    [:h2 "Button with pending state"]

    (when-let [event (ui/suspense
                       (p/fn [Effect]
                         (ui/button {:on-click (p/fn [event] (ui/event :click ~@(p/wrap run-long-task!)))}
                           (Effect. (p/fn [pending?] (dom/props {:disabled pending?})))
                           (dom/text "Long running task (log to console)"))))]
      (prn "clicked! " event))


    [:hr]
    [:h2 "Checkbox"]

    [:label
     (let [checked? (ui/checkbox {:checked   true
                                  ::ui/value (p/fn [value] value)})]
       (dom/text " Checked? " checked?))]

    [:hr]
    [:h2 "Numeric input"]

    [:span (let [value (ui/numeric-input {:format    "%.2f"
                                          :step      0.5
                                          :value     (/ 10 3)
                                          ::ui/value (p/fn [value] value)})]
             (dom/text value))]

    [:hr]
    [:h2 "Text input"]
    [:span (let [value (ui/input {:placeholder "Text …"
                                  :value       "init"
                                  ::ui/value   (p/fn [value] value)})]
             (dom/text value))]

    [:hr]
    [:h2 "Date"]
    [:span (let [num (ui/input {:placeholder "Date …"
                                :type        :datetime-local
                                ::ui/value   (p/fn [value] value)})]
             (dom/text num))]

    [:hr]
    [:h2 "Select"]
    [:span (let [selected (ui/select {:value     {:value 0, :text "Initial"}
                                      :options   [{:value 1, :text "One"}
                                                  {:value 2, :text "Two"}
                                                  {:value 3, :text "Three"}]
                                      ::ui/value (p/fn [value] value)})]
             (dom/text selected))]

    [:hr]
    [:h2 "Native Typeahead"]

    [:span (let [value (ui/native-typeahead {:placeholder "Search…"
                                             :options     (p/fn [needle]
                                                            (query-names (or needle "")))})]
             (dom/text value))]

    ]))

(def main #?(:cljs (p/client (p/main
                              (try
                                (binding [dom/node (dom/by-id "root")]
                                  (App.))
                                (catch Pending _))))))


(comment
  #?(:clj (user/browser-main! `main))
  )