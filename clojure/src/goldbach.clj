(ns goldbach
  (:require [clj-time [core :as t]]
            [taoensso.carmine :as car :refer (wcar)]))

(def PRIMES "primes") ; Redis primes set name
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

(defn -main []
  (let [start-time (t/now)
        smallest-answer (smallest-non-goldbach)
        run-time (t/in-millis (t/interval start-time (t/now)))]
    (println (str "Smallest odd composite number without a Goldbach solution: " smallest-answer))
    (println (str "Actual run time: " run-time "ms."))))
