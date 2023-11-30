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
  (println (str (name (first service)) ".deploy.yml") (gen-string service)))

(defn gen-ports
  [ports]
  (loop [port ports
         ret ()]
    (if (empty? port)
      (println (yaml/generate-string (ordered-map :ports ret) :dumper-options {:flow-style :block}))
      (recur (rest port) (conj ret (ordered-map :containerPort (first (str/split (last (str/split (first port) #":")) #"/")) :protocol (if (re-find #"udp$" (first port)) "UDP" "TCP")))))))

(defn seq-range
  [min max]
  (loop [incr min
         ret ()]
    (if (> incr max)
      ret
      (recur (inc incr) (conj ret incr)))))

(defn check-range
  [port udp?]
  (let [vals (map #(Integer/parseInt %) (str/split (first (str/split port #"/")) #"-"))]
    (if (< (first vals) (second vals))
      (map #(str % (when udp? "/udp")) (seq-range (first vals) (second vals)))
      ((println (str "error : " (first vals) " superior than " (second vals)))))))

(defn validates-port
  [arg]
  (let [port (last (str/split arg #":"))]
    (if (re-matches #"[0-9]{1,5}(-[0-9]{1,5})?(\/udp)?" port)
      (if (re-find #"-" port)
              (check-range port (re-find #"/udp" port))
              (list port))
      ((println (str "error parsing ports : " port))
       (System/exit 1)))))

(defn red-ports
  [ports]
  (reduce (fn [p n] (into p (validates-port n))) () ports))

(defn switch-opts
  [elem service]
  (case elem
    :ports (gen-ports (red-ports (elem service)))
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
