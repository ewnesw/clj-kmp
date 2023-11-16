(ns clj-kmp.parse
 (:require [clj-yaml.core :as yaml]
           [clojure.walk :as walk]))

(defn parse-file
  [file]
  (yaml/parse-string (slurp file)))

(def base-deploy (parse-file "resources/deploy.yml"))

(defn walk-replace
  [map val replace]
  (walk/postwalk (fn [elem] (if (= elem val) replace elem)) map))

(defn gen-deploy
  [service]
  (let [svc (first service)]
    (walk-replace (walk-replace base-deploy "service_name" (first svc)) "image_name" (get-in (second svc) [:image]))))

(defn gen-string
  [service]
  (yaml/generate-string (gen-deploy service) :dumper-options {:flow-style :block}))

(defn gen-file
  [service]
  (spit (str (name (first (first service))) ".deploy.yml") (gen-string service)))

(defn check-config
  [service]
  (loop [elem [:ports :volumes :environment]
         ret []]
    (if (empty? elem)
      (println ret)
      (recur (drop 1 elem) (if (contains? service elem) (conj ret elem) ret)))))

(defn gen-manifest
  [service]
  (if (empty? (check-config service))
              (gen-file service)
              (println "oe")))

(defn parse-services
  [file]
  (loop [service (:services (parse-file file))]
    (when-not (empty? service)
      (gen-manifest service)
      (recur (apply dissoc service (first service))))))

(check-config (:seafile (:services (parse-file "resources/docker-compose.seafile.yml"))))
