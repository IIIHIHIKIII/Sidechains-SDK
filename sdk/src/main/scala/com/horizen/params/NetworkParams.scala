package com.horizen.params

import java.math.BigInteger

import scorex.util.{ModifierId, bytesToId}

trait NetworkParams {
  // Mainchain ProofOfWork parameters:
  val EquihashN: Int
  val EquihashK: Int
  val EquihashVarIntLength: Int // VarInt value length for Equihash solution bytes
  val EquihashSolutionLength: Int // solution bytes length

  val powLimit: BigInteger

  val nPowAveragingWindow: Int
  val nPowMaxAdjustDown: Int
  val nPowMaxAdjustUp: Int
  val nPowTargetSpacing: Int
  final def averagingWindowTimespan: Int = nPowAveragingWindow * nPowTargetSpacing
  final def MinActualTimespan: Int = (averagingWindowTimespan * (100 - nPowMaxAdjustUp  )) / 100
  final def MaxActualTimespan: Int = (averagingWindowTimespan * (100 + nPowMaxAdjustDown)) / 100
  final def nMedianTimeSpan: Int = 11


  // Sidechain params:
  val zeroHashBytes: Array[Byte] = new Array[Byte](32)
  val sidechainId: Array[Byte]
  val sidechainGenesisBlockId: ModifierId
  val sidechainGenesisBlockParentId: ModifierId = bytesToId(new Array[Byte](32))

  val maxHistoryRewritingLength: Int = 100


  // Sidechain genesis params:
  val genesisMainchainBlockHash: Array[Byte] // hash of the block which include SidechainCreationTx for current SC
  val genesisPoWData: Seq[(Int, Int)] // Tuples with timestamps and bits values of <nPowAveragingWindow> blocks up-to <genesisMainchainBlockHash> block. From oldest MC block to genesis one.
  val mainchainCreationBlockHeight: Int // Height of the block which include SidechainCreationTx for current SC
}
