(ns clj-kmp.core
  (:require [clj-kmp.args :as arg]
            [clj-kmp.parse :as parse]
            ))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [{:keys [file options exit-message ok?]} (arg/validate-args args)]
    (when exit-message
      (arg/exit (if ok? 0 1) exit-message))
    (parse/parse-services file)))
