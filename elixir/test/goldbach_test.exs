defmodule GoldbachTest do
  use ExUnit.Case
  import Goldbach
  doctest Goldbach

  test "check_possible_solution" do
    assert check_possible_solution(9, 7, 1)
    assert check_possible_solution(15, 7, 2)
    assert check_possible_solution(15, 13, 1)
    assert check_possible_solution(27, 19, 2)
    assert check_possible_solution(33, 31, 1)

    refute check_possible_solution(15, 11, 1)
    refute check_possible_solution(33, 19, 3)

    assert_raise FunctionClauseError, fn ->
      check_possible_solution(19, 33, 3)
    end
  end

  test "primes_below" do
    assert primes_below(10) == [2, 3, 5, 7]
  end

  test "not_prime?" do
    assert not_prime?(4)
    refute not_prime?(11)
  end

  test "solution_for_odd_comp_and_prime" do
    assert solution_for_odd_comp_and_prime(15, 5) == nil

    assert solution_for_odd_comp_and_prime(15, 7) == {7, 2}
    assert solution_for_odd_comp_and_prime(15, 13) == {13, 1}
  end

  test "solution_for_odd_comp" do
    assert solution_for_odd_comp(15) == {13, 1}
    assert solution_for_odd_comp(27) == {19, 2}
  end

  test "odd_composite_numbers" do
    assert Enum.take(odd_composite_numbers(), 6) == [9, 15, 21, 25, 27, 33]
  end

  test "smallest_non_goldbach" do
    assert smallest_non_goldbach() == 5777
  end
end
