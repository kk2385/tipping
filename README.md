# tipping

A Program that plays the No Tipping Game.

Rules are as follows (copy and pasted from http://cs.nyu.edu/courses/fall11/CSCI-GA.2965-001/notipping.html):

Description

Given a uniform, flat board (made of a titanium alloy) 30 meters long and weighing 3 kilograms, consider it ranging from -15 meters to 15 meters. So the center of gravity is at 0. We place two supports of equal heights at positions -3 and -1 and a 3 kilogram block at position -4.

The No Tipping game is a two person game that works as follows: you and I each start with 10 blocks having weights 1 kg through 10 kg. (10 should be a parameter -- no magic numbers in your programs please). The first player places one block anywhere on the board, then the second player places one block anywhere on the board, and play alternates with each player placing one block until the second player places the last block. (You may not place one block above another one, so each position will have at most one block.) If after any ply, the placement of a block causes the board to tip, then the player who did that ply loses. Suppose that the board hasn't tipped by the time the last block is placed. Then the players remove one block at a time (a player may remove a block originally placed by the other player) in turns. If the board tips following a removal, then the player who removed the last block loses.

As the game proceeds, the net torque around each support is calculated and displayed. The blocks, whether on the board or in the possession of the players, are displayed with their weight values.
