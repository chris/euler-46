(ns goldbach
  (:require [clojure.core.async :as async :refer [>!! <!! chan go go-loop]]
            [clj-time [core :as t]]
            [taoensso.carmine :as car :refer (wcar)]))

(def PRIMES "euler-46:primes") ; Redis primes set name
(def server-conn {:pool {} :spec {}}) ; See `wcar` docstring for opts
(defmacro wcar* [& body] `(car/wcar server-conn ~@body))

(defn check-possible-solution
  "Returns true if the candidate prime and square equal the odd-comp per Goldbach's formula."
  [odd-comp prime num-to-square]
  {:pre [(> odd-comp prime)]}
  (= odd-comp (+ prime (* 2 (* num-to-square num-to-square)))))

(defn primes-below
  [x]
  (map #(Integer/parseInt %) (wcar* (car/zrangebyscore PRIMES 0 x))))

(defn primes-for-odd-comp
  "Returns ordered collection of primes eligible for use when checking a given odd composite number"
  [odd-comp]
  (primes-below (- odd-comp 2)))

(defn solution-for-odd-comp-and-prime
  "Returns vector with the prime and the square base that solves for odd-comp,
   or nil if there is no solution for this prime and odd-comp."
  [odd-comp prime]
  (let [square-bases (range 1 (+ (Math/floor (/ (- odd-comp prime) 2)) 1))
        solution (first (filter #(check-possible-solution odd-comp prime %) square-bases))]
    (when solution [prime solution])))

(defn solution-for-odd-comp
  "Find a Goldbach soluton for a given odd composite number.
   Returns a vector with the prime and the square base that solves for odd-comp."
  [odd-comp]
  (let [primes (-> odd-comp primes-for-odd-comp reverse)]
     (some #(solution-for-odd-comp-and-prime odd-comp %) primes)))

(defn not-prime? [n] (nil? (wcar* (car/zscore PRIMES n))))

(defn next-odd-composite
  "Return the next odd composite number after the argument."
  [n]
  (let [next-odd (+ 2 n)]
    (if (not-prime? next-odd) next-odd (next-odd-composite next-odd))))

(defn odd-composite-numbers
  "Returns a lazy sequence of odd composite numbers (non prime odds)."
  ([] (odd-composite-numbers 9))
  ([n] (lazy-seq (cons n (odd-composite-numbers (next-odd-composite n))))))

(defn smallest-non-goldbach
  "Finds the answer - the smallest odd composite to not be solvable by Goldbach's conjecture"
  []
  (first (filter #(nil? (solution-for-odd-comp %)) (odd-composite-numbers))))

(def source-numbers (chan))
(def answer-channel (chan))

(defn smallest-non-goldbach-using-channels
  "Finds the answer, but this one uses channels to feed the test numbers in, and then to
   publish the answer, in theory to speed it up by running solvers in parallel."
  []
  ;; Setup 3 go-blocks to test odd-composites in parallel to find the answer
  ;; Puts the answer on answer-channel as soon as one is found (presumes the first
  ;; solution is correct, although technically we should let each block finish that
  ;; round to be sure there aren't two answers really close to each other, where one
  ;; of the higher answer blocks finishes before the other)
  (dotimes [_ 3]
    (go-loop [num (<!! source-numbers)]
      (let [result (solution-for-odd-comp num)]
        (when (nil? result) (>!! answer-channel num)))
      (recur (<!! source-numbers))))
  ;; Feed our candidate odd conposite numbers into the source-numbers channel
  (go (doseq [num (odd-composite-numbers)] (>!! source-numbers num)))
  ;; Grab the answer as soon as it's available, and we'll be done.
  ;; The go-blocks will still be running, but our main thread will finish
  ;; executing, and thus end the program.
  (<!! answer-channel))

(defn print-answer
  [label answer run-time]
  (println
      (str "Smallest odd composite number without a Goldbach "
           label
           " solution: "
           answer
           ", run time: "
           run-time
           "ms.")))

(defn -main []
  (let [start-time1 (t/now)
        answer1 (smallest-non-goldbach)
        run-time1 (t/in-millis (t/interval start-time1 (t/now)))
        start-time2 (t/now)
        answer2 (smallest-non-goldbach-using-channels)
        run-time2 (t/in-millis (t/interval start-time2 (t/now)))]
    (print-answer "standard" answer1 run-time1)
    (print-answer "channel/async" answer2 run-time2)))
