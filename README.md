# Euler problem 46 - Goldbach conjecture

Code to solve this Euler problem and experiment with some languages and designs.

## Notes on my general solution approach

I put primes up to 1,000,000 into a Redis sorted set (where the score is the same as the prime). This allowed for easy retrieval of primes up to a certain value, as well as is faster than a regular DB. I also did this because I'd been thinking about building this as a way to test Lambda functions in various languages, and I wanted at least one external dependency to make it a more realistic test of cold start time and running time with dependencies (since almost anything I'd build would likely have at least one or two external dependencies, and likely a DB of some sort).

An "odd composite number" is a non-prime odd number.

To do the actual solving, I determined first that for a given odd composite number:
-  The largest prime we can use is the odd composite minus 2 (since you have a minimum `2(1^2)=2` you have to add to it).
- The range of base numbers to use for the squared component is `floor((odd-composite - prime) / 2)`.

These two aspects allow then constraining the values to test for each odd composite.

So far I've started with the largest prime and worked backwards. It should be noted that there are (many?) multiple solutions for a given odd composite. e.g. 15 can be solved with `7 + 2(2^2)` or `13 + 2(2^1)`.

I had assumed that this might take a bit of time to run and find a solution, and thus my thought was that I'd try a solution (which I think works in most of the languages I am interested in trying this for) where I create a channel, and inject the odd composites into the channel. Then, have a few go-blocks running, that look for solutions. Each go-block, when ready, pulls a number from the channel, and then sees if it can find a solution. If not, it reports that as an answer and stops. The other go-blocks would finish the number they were working on (presumes they all stay fairly close in time), in case that number was also possibly an answer, and maybe is a smaller number than the other go-block that found one, but then seeing that an answer has been found, when they're done with that round, they too quit, and then the answer(s) are returned.

It turned out that at least the initial Clojure implementation was so fast that doing the above is unnecessary, however the channel/parallel variant did cut the time in half. The standard solution (without using go-blocks and parallel processing) took about 1200ms. The go-blocks version takes just under half that time (averaged around 560ms), whether using 2, 3, or 4 go-blocks.

The Elixir implementation, for a "standard" (non-concurrent) approach, is significantly slower. I may not have all lazy-sequence use or some such that could be causing part of it, but the actual computation time is around 6 seconds (vs. just over 1s for Clojure). Elixir/Erlang are not known for speedy math, but this is dramatic. Of course, the actual runtime of starting the program, and getting an answer on the command line is just about the same between Clojure & Elixir, given Clojure/Java's significant VM startup time (nearly all of that time is JVM startup, whereas nearly all the of the runtime for Elixir is it actually computing the answer!). This timing may also be affected by the (not yet solved) re-creation of the Redis connection every time we issue a Redis command.

## Benchmarks

Note, these aren't true Benchmarks (i.e. I didn't do thousands of samples, etc.), but more for a general gist.

Solution type | Clojure | Elixir
------------- | ------- | ------
non-concurrent, command line program time | 8s | 1.7s
non-concurrent, pure computation time | 1.2s | 1.5s
concurrent, command line program time | 7s |
concurrent, pure computation time | 560ms |

## Redis setup

Run `./redis_primes.rb` with the `primes_upto_1million.txt` file (you won't need more than this, or really even half this) to populate the primes in Redis:
```
./redis_primes.rb primes_upto_1million.txt
```
 This adds them to a sorted set called "euler-46:primes". You need to have Redis running locally for this to work. You also need to have the `redis` gem installed and the proper Ruby version per `.ruby-version`.

## Clojure

To run it/find the answer:
```
clj -m goldbach
```

To run the tests:
```
clj -Atest
```

Used lazy sequence for the odd composite generation, so we're only generating what we need.


## Elixir

To run tests:

`mix test`

To run it/find answer, you need to compile it and then run it:
```
mix escript.build && ./euler
```

### Notes:

Initially wasn't sure how to create a single persistent connection to Redis in Elixir. This meant that every prime check (call to `not_prime?`), or fetching of primes, was re-creating the connection. My suspicion was that this was a major cause of the slower speed of the Elixir solution. Figured out that needed to make the program an `Application`, and start a supervision tree, with a Redix connection child instance, based on [these instructions](https://hexdocs.pm/redix/real-world-usage.html#global-redix). To do this, added `use Application` to the Euler.CLI module, and then added a `start` function which setup the Redis connection as a named (`:redix`) child process. That name can then be passed to uses of `Redix.command` so it uses that instance. This *massively* sped things up, so that the program ran in about 1.5-2 seconds (instead of about 7+), and about 1.5s for the computation time instead of 6s.

### Todo:

- [ ] Create single Redis connection to use by functions that use Redis.
- [ ] Implement a concurrent approach.


## Notes Comparing Languages

* Clojure's startup time (e.g. to run the program, or run tests not at a REPL) is painful - about 7 seconds.
* Elixir's startup time is basically instant - programs and tests ran instantly.
* I get used to not using commas for parameters to functions (in Clojure) quickly!
* Elixir doc strings, and in particular doc string tests (i.e. where it runs the example code in the docstring) is pretty cool. For simple functions can probably not write tests, and just have a positive and negative example or similar?
* Elixir runs tests in parallel by default?
* Clojure's `spec` is pretty amazing, in that you can get very detailed/specific in terms of spec'ing a data structure or function behavior. Haven't seen anything else like this.
* Elixir's guards are great, and provide sort of a small bit of spec.
* Really really like Elixir/Erlang standard of returning a tuple such as {:ok, value} to indicate error cases, and then being able to pattern match on that. Clojure's error handling has no standardization, and it has exceptions, but most folks seem to not want to use exceptions. I've used a 3rd party library to do some better error handling and allow pipelining where errors may occur along the way, but it's not that intuitive, and is not a standard/idiom.
* Clojure handles and uses lazy sequences easily and/or by default. So, most collection handling functions just work with lazy-seq's easily. Elixir splits these out where you're using either functions from Enum, or functions from Stream. However, that said, it's basically just as easy, and maybe a bit more clear that you're working with lazy items (you also don't run into the problem where sometimes something won't be realized in Clojure because it's lazy and you need to do an operation that is sure to realize a result).
* Elixir has string substitution same as Ruby, Clojure have to use `str` to concat strings.
