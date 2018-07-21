defmodule Euler.CLI do
  def print_answer(label, answer, runtime) do
    IO.puts("Smallest odd composite number without a Goldbach #{label} solution: #{answer},
      run time: #{runtime}ms.")
  end

  def main(_args) do
    start_time = DateTime.utc_now()
    answer1 = Goldbach.smallest_non_goldbach()
    run_time1 = DateTime.diff(DateTime.utc_now(), start_time, :milliseconds)
    print_answer("standard", answer1, run_time1)
  end
end
