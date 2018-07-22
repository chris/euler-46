defmodule Goldbach do
  import Enum, only: [map: 2, filter: 2, reverse: 1]
  import Float, only: [floor: 1]

  @primes "euler-46:primes"

  @moduledoc """
  Euler problem 46 - Goldbach's conjecture.
  """

  @doc """
  Returns true if the candidate prime and square equal the odd-comp per Goldbach's formula.

  ## Examples

      iex> Goldbach.check_possible_solution(9, 7, 1)
      true

  """
  @spec check_possible_solution(pos_integer, pos_integer, pos_integer) :: boolean
  def check_possible_solution(odd_comp, prime, num_to_square) when odd_comp > prime do
    odd_comp == prime + 2 * (num_to_square * num_to_square)
  end

  def not_prime?(num) when is_integer(num) do
    case Redix.command(:redix, ["ZSCORE", @primes, "#{num}"]) do
      {:ok, nil} -> true
      {:ok, _} -> false
    end
  end

  @doc """
  Return prime numbers below x
  """
  def primes_below(x) when is_integer(x) and x > 2 do
    {:ok, p} = Redix.command(:redix, ["ZRANGEBYSCORE", @primes, "0", "#{x}"])
    map(p, &String.to_integer/1)
  end

  @doc """
  Returns ordered collection of primes eligible for use when checking a given
  odd composite number.
  """
  def primes_for_odd_comp(odd_comp) when is_integer(odd_comp), do: primes_below(odd_comp - 2)

  @doc """
  Returns vector with the prime and the square base that solves for odd-comp,
   or nil if there is no solution for this prime and odd-comp.
  """
  def solution_for_odd_comp_and_prime(odd_comp, prime) do
    square_bases = 1..trunc(floor((odd_comp - prime) / 2.0))
    check = &check_possible_solution(odd_comp, prime, &1)

    case List.first(filter(square_bases, &check.(&1))) do
      nil -> nil
      solution -> {prime, solution}
    end
  end

  @doc """
  Find a Goldbach soluton for a given odd composite number.
  Returns a vector with the prime and the square base that solves for odd-comp.
  """
  def solution_for_odd_comp(odd_comp) when is_integer(odd_comp) do
    odd_comp
    |> primes_for_odd_comp
    |> reverse
    |> Stream.map(&solution_for_odd_comp_and_prime(odd_comp, &1))
    |> Stream.reject(&is_nil/1)
    |> Enum.take(1)
    |> List.first()
  end

  def next_odd_composite(n) do
    if not_prime?(n + 2), do: n + 2, else: next_odd_composite(n + 2)
  end

  @doc """
  Return a stream of odd composite numbers.
  """
  def odd_composite_numbers do
    Stream.unfold(9, &{&1, next_odd_composite(&1)})
  end

  @doc "Finds the answer - the smallest odd composite to not be solvable by Goldbach's conjecture"
  def smallest_non_goldbach() do
    odd_composite_numbers()
    |> Stream.filter(&is_nil(solution_for_odd_comp(&1)))
    |> Enum.take(1)
    |> List.first()
  end
end
