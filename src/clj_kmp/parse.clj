(ns clj-kmp.parse
 (:require [clj-yaml.core :as yaml]
           [clojure.walk :as walk]
           [flatland.ordered.map :refer (ordered-map)]
           [clojure.string :as str]))

(defn parse-file
  [file]
  (yaml/parse-string (slurp file)))

(def base-deploy (parse-file "resources/deploy.yml"))

(def manifest-options [:ports :volumes :environment])

(defn walk-replace
  [map val replace]
  (walk/postwalk (fn [elem] (if (= elem val) replace elem)) map))

(defn gen-deploy
  [service]
  (let [svc service]
    (walk-replace (walk-replace base-deploy "service_name" (first svc)) "image_name" (get-in (second svc) [:image]))))

(defn gen-string
  [service]
  (yaml/generate-string (gen-deploy service) :dumper-options {:flow-style :block}))

(defn gen-file
  [service options]
  (spit (str (name (first service)) ".deploy.yml") (gen-string service)))

(defn gen-ports
  [ports]
  (loop [port ports
         ret ()]
    (if (empty? port)
      (println (yaml/generate-string (ordered-map :ports ret) :dumper-options {:flow-style :block}))
      (recur (rest port) (conj ret (ordered-map :containerPort (first (str/split (last (str/split (first port) #":")) #"/")) :protocol (if (re-find #"udp$" (first port)) "UDP" "TCP")))))))

(defn seq-port
  [port]
  (let [vals (str/split port #"-")]
    (if (< (first vals) (second vals))
      (println "temp yes")
      (println "temp no"))))

(defn aled
  [arg]
  (let [port (str/split arg #":")]
    (if (re-find #"[0-9]{1,5}(-[0-9]{1,5})?(\/udp)?" port)
      (if (re-find #"-" port)
              (seq-port port)
              port)
      ((println (str "error parsing ports : " port))
       (System/exit 1)))))

(defn validate-ports
  [ports]
  (reduce (fn [p n] (conj p (aled n))) () ports))

(defn switch-opts
  [elem service]
  (case elem
    :ports (gen-ports (elem service))
    :environment (println "env")
    :volumes (println "volumes")
    ))

(defn check-config
  [service]
  (let [svc (second service)]
    (loop [elem manifest-options 
           ret {}]
      (if (empty? elem)
        ret
        (recur (drop 1 elem) (if (contains? svc (first elem)) (conj ret (switch-opts (first elem) svc)) ret))))))

(defn parse-services
  [file]
  (loop [service (:services (parse-file file))]
    (when-not (empty? service)
      (gen-file (first service) (check-config (first service)))
      (recur (apply dissoc service (first service))))))

(parse-services "resources/docker-compose.seafile.yml")
