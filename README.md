# Euler problem 46 - Goldbach conjecture

Code to solve this Euler problem and experiment with some languages and designs.

Answer to the problem is 5777.

## Notes on my general solution approach

I put primes up to 1,000,000 into a Redis sorted set (where the score is the same as the prime). This allowed for easy retrieval of primes up to a certain value, as well as is faster than a regular DB. I also did this because I'd been thinking about building this as a way to test Lambda functions in various languages, and I wanted at least one external dependency to make it a more realistic test of cold start time and running time with dependencies (since almost anything I'd build would likely have at least one or two external dependencies, and likely a DB of some sort).

An "odd composite number" is a non-prime odd number.

To do the actual solving, I determined first that for a given odd composite number:
-  The largest prime we can use is the odd composite minus 2 (since you have a minimum `2(1^2)=2` you have to add to it).
- The range of base numbers to use for the squared component is `floor((odd-composite - prime) / 2)`.

These two aspects allow then constraining the values to test for each odd composite.

So far I've started with the largest prime and worked backwards. It should be noted that there are (many?) multiple solutions for a given odd composite. e.g. 15 can be solved with `7 + 2(2^2)` or `13 + 2(2^1)`.

I had assumed that this might take a bit of time to run and find a solution, and thus my thought was that I'd try a solution (which I think works in most of the languages I am interested in trying this for) where I create a channel, and inject the odd composites into the channel. Then, have say 4 go-blocks/threads running, that look for solutions. Each go-block, when ready, pulls a number from the channel, and then sees if it can find a solution. If not, it reports that as an answer and stops. The other go-blocks would finish the number they were working on (presumes they all stay fairly close in time), in case that number was also possibly an answer, and maybe is a smaller number than the other go-block that found one, but then seeing that an answer has been found, when they're done with that round, they too quit, and then the answer(s) are returned.

It turned out that at least the initial Clojure implementation was so fast that doing the above is probably not going to gain anything. However, I may still try it just to practice use of channels/go-blocks, etc., and then also to be able to compare this design in other languages.

## Clojure

Initial version with just basically brute force going through odd composites was really fast - takes about 8 seconds, including JVM startup time, etc. and really only ~1.2 seconds (1147-1229ms) per the actual code runtime (see code where it does/outputs the timing).


