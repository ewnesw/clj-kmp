(ns kompose.args
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [kompose.file :as kf]))

(def cli-options
  ;; An option with a required argument
  [;;["-i" "--ingress HOST" "create ingress based on give host, remember to check the file to see if it fits your setup"
    ;;:validate [#(first (re-find #"^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]))*$" %)) #(str % " hostname not valid")]]
   ["-h" "--help"]])


(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn usage [options-summary]
  (->> ["This is my program. There are many like it, but this one is mine."
        ""
        "Usage: program-name [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start    Start a new server"
        "  stop     Stop an existing server"
        "  status   Print a server's status"
        ""
        "Please refer to the manual page for more information."]
       (string/join \newline)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ;  help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      (and (= 1 (count arguments)) (kf/file-lookup (first arguments)))
      {:file (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)}
      )))

(defn exit [status msg]
  (println msg)
  (System/exit status))
