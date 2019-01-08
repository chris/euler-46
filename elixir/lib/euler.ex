defmodule Euler.CLI do
  use Application

  def start(_type, _args) do
    IO.puts("doing start in Euler.CLI")

    children = [
      {Redix, [[], [name: :redix]]}
    ]

    opts = [strategy: :one_for_one, name: Euler.Supervisor]
    Supervisor.start_link(children, opts)
  end

  def print_answer(label, answer, runtime) do
    IO.puts("Smallest odd composite number without a Goldbach #{label} solution: #{answer},
      run time: #{runtime}ms.")
  end

  def main(_args) do
    start_time = DateTime.utc_now()
    answer1 = Goldbach.smallest_non_goldbach()
    run_time1 = DateTime.diff(DateTime.utc_now(), start_time, :milliseconds)
    print_answer("standard", answer1, run_time1)

    start_time2 = DateTime.utc_now()
    {:ok, a} = OddCompProducer.start_link(9)
    {:ok, b} = GoldbachChecker.start_link(:B1)
    {:ok, b2} = GoldbachChecker.start_link(:B2)
    {:ok, b3} = GoldbachChecker.start_link(:B3)
    {:ok, c} = AnswerConsumer.start_link()
    run_time2 = DateTime.diff(DateTime.utc_now(), start_time, :milliseconds)
    print_answer("concurrent", answer1, run_time2)
  end
end
