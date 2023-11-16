(ns clj-kmp.file
  (:require [clojure.java.io :as io]))

(defn file-lookup [path]
  (->> path 
       clojure.java.io/file
       .exists))

