package com.horizen.chain

import java.io.{PrintWriter, StringWriter}

import com.horizen.fixtures.SidechainBlockInfoFixture
import org.junit.Assert.{assertEquals, assertFalse, assertNotEquals, assertTrue}
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import scorex.util.ModifierId

import scala.collection.breakOut
import scala.util.Try

class ActiveChainTest extends JUnitSuite with SidechainBlockInfoFixture {
  var testSeed: Long = 92830932726517L
  val genesisBlockMainchainHeight = 42

  private def assertFunctionResultsAreEqualsForGivenInput[T, S](fun1: T => S, fun2: T => S, input: Seq[T], message: String = ""): Unit = {
    input.foreach { in =>
      val res1 = fun1(in)
      val res2 = fun2(in)
      assertEquals(s"Failed for ${in} " + message, res1, res2)
    }
  }

  private def addNewBestBlockIsSuccessful(chain: ActiveChain,
                                          id: ModifierId,
                                          data: SidechainBlockInfo,
                                          mainchainParent: Option[MainchainBlockReferenceId]): Unit = {
    val adding = Try {chain.setBestBlock(id, data, mainchainParent)}
    val errorMessage = if (adding.isSuccess) {
      ""
    }
    else {
      val ex = adding.failed.get
      val sw = new StringWriter
      ex.printStackTrace(new PrintWriter(sw))
      sw.toString
    }
    assertTrue(s"Failed to add element to an ActiveChain due ${errorMessage}", adding.isSuccess)
  }

  private def addNewBestBlockShallBeFailed(chain: ActiveChain,
                                           id: ModifierId,
                                           data: SidechainBlockInfo,
                                           mainchainParent: Option[MainchainBlockReferenceId]): Unit = {
    val adding = Try {chain.setBestBlock(id, data, mainchainParent)}
    assertTrue(s"Element expected to not be added to the ActiveChain", adding.isFailure)
  }

  private def checkEmptyActiveChain(chain: ActiveChain): Unit = {
    assertEquals("Empty ActiveChain expected to have height 0", 0, chain.height)
    assertTrue("Empty ActiveChain expected to have no tip", chain.bestId.isEmpty)
    assertTrue("Empty ActiveChain expected to have no tipInfo", chain.bestScBlockInfo.isEmpty)

    assertTrue("Empty ActiveChain expected not to find height of nonexistent modifier", chain.heightById(getRandomModifier()).isEmpty)
    assertFalse("Empty ActiveChain expected not to contain nonexistent modifier", chain.contains(getRandomModifier()))
    assertTrue("Empty ActiveChain expected not to find nonexistent modifier", chain.blockInfoById(getRandomModifier()).isEmpty)
    assertTrue("Empty ActiveChain expected not to find modifier for inconsistent height", chain.blockInfoByHeight(1).isEmpty)
    assertTrue("Empty ActiveChain expected not to find modifier id for inconsistent height", chain.idByHeight(0).isEmpty)
    assertTrue("Empty ActiveChain expected not to find modifier id for inconsistent height", chain.idByHeight(1).isEmpty)
    assertTrue("Empty ActiveChain expected to return empty chain from nonexistent modifier", chain.chainAfter(getRandomModifier()).isEmpty)
  }

  private def checkElementIsPresent(chain: ActiveChain,
                                    id: ModifierId,
                                    data: SidechainBlockInfo,
                                    height: Int,
                                    mainchainInitialHeight: Int,
                                    allMainchainReferences: Seq[MainchainBlockReferenceId]
                                   ): Unit = {
    assertTrue("Chain from shall not be empty for added element", chain.chainAfter(id).nonEmpty)
    assertTrue("Element shall be present in chain", chain.contains(id))
    assertEquals("Data shall be reachable by height", data, chain.blockInfoByHeight(height).get)
    assertEquals("Data shall be reachable by id", data, chain.blockInfoById(id).get)
    assertEquals("Height of added element shall be defined", height, chain.heightById(id).get)
    assertEquals("Id of added element shall reachable by height", id, chain.idByHeight(height).get)

    data.mainchainBlockReferenceHashes.zipWithIndex.foreach {
      case(ref, index) =>
        val mcHeight = mainchainInitialHeight + index + genesisBlockMainchainHeight

        assertEquals("Sidechain height shall be defined for added mainchain", height, chain.heightByMcId(ref).get)
        assertEquals("Height of mainchain shall be correctly defined", mcHeight, chain.mcHeightByMcId(ref).get)
        assertEquals("Sidechain id shall be found my mainchainId", id, chain.idByMcId(ref).get)
        assertEquals("Mainchain id shall be correctly get by height", ref, chain.mcIdByMcHeight(mcHeight).get)

        val parentIndex = mcHeight - genesisBlockMainchainHeight
        if (parentIndex > 0) {
          assertEquals("Mainchain data by mainchain id shall be as expected", allMainchainReferences(parentIndex - 1), chain.mcBlockReferenceDataByMcId(ref).get.getParentId)
        }
      }
  }

  private def checkElementIsNotPresent(chain: ActiveChain,
                                       id: ModifierId,
                                       data: SidechainBlockInfo,
                                       height: Int): Unit = {
    assertFalse("Element expected not be present", chain.contains(id))

    val referencesInActualDataIsEmpty = chain.blockInfoByHeight(height).exists(data => data.mainchainBlockReferenceHashes.isEmpty)

    // Do not check data if expected and actual data no have main chain references due false positive
    if (!(data.mainchainBlockReferenceHashes.isEmpty && referencesInActualDataIsEmpty)) {
      assertNotEquals("Data shall not be found", Some(data), chain.blockInfoByHeight(height))
    }
  }

  private def checkElementIsBest(chain: ActiveChain,
                                 id: ModifierId,
                                 data: SidechainBlockInfo,
                                 height: Int): Unit = {
    assertEquals("Data shall be best", data, chain.bestScBlockInfo.get)
    assertEquals("Id shall be best", id, chain.bestId.get)
    assertEquals("Height of activechain shall be as expected", height, chain.height)
  }

  private def checkElementIsNotBest(chain: ActiveChain,
                                    id: ModifierId,
                                    data: SidechainBlockInfo,
                                    height: Int): Unit = {
    assertNotEquals("Data shall be best", data, chain.bestScBlockInfo.get)
    assertNotEquals("Id shall be best", id, chain.bestId.get)
    assertNotEquals("Height of activechain shall be as expected", height, chain.height)
  }

  @Test
  def checkEmptyChain(): Unit = {
    val chain = ActiveChain(genesisBlockMainchainHeight)
    checkEmptyActiveChain(chain)
  }

  @Test
  def checkFilledByDataChain(): Unit = {
    setSeed(testSeed)
    val chainHeight: Int = 10
    val generatedData = generateDataSequenceWithGenesisBlock(chainHeight)
    val initialParent = generatedData.flatMap(_._3).head
    val allAtOnceChain = ActiveChain(generatedData.map{case (id, sbInfo, _) => (id, sbInfo)}(breakOut), initialParent, genesisBlockMainchainHeight)

    val filledByBestBlock = ActiveChain(genesisBlockMainchainHeight)
    generatedData.foldLeft(Option(initialParent)) {
      case (parent, (id, info, mainchainParent)) =>
        val addRes = Try{filledByBestBlock.setBestBlock(id, info, mainchainParent)}
        if (addRes.isFailure) println(addRes.failed)
        assertTrue(addRes.isSuccess)
        info.mainchainBlockReferenceHashes.lastOption.orElse(parent)
    }

    val usedIds = generatedData.map(_._1)
    val mainchainIds = generatedData.flatMap(_._3)
    val usedHeights = 1 to chainHeight

    assertFunctionResultsAreEqualsForGivenInput(allAtOnceChain.contains, filledByBestBlock.contains, usedIds, "contains is different")
    assertFunctionResultsAreEqualsForGivenInput(allAtOnceChain.heightById, filledByBestBlock.heightById, usedIds, "height is different")
    assertFunctionResultsAreEqualsForGivenInput(allAtOnceChain.heightByMcId, filledByBestBlock.heightByMcId, mainchainIds, "sidechain height by mainchain id is different")
    assertFunctionResultsAreEqualsForGivenInput(allAtOnceChain.mcHeightByMcId, filledByBestBlock.mcHeightByMcId, mainchainIds, "mainchain height is different")
    assertFunctionResultsAreEqualsForGivenInput(allAtOnceChain.blockInfoById, filledByBestBlock.blockInfoById, usedIds, "data is different")
    assertFunctionResultsAreEqualsForGivenInput(allAtOnceChain.blockInfoByHeight, filledByBestBlock.blockInfoByHeight, usedHeights)

    val mainchainReferences = generatedData.map(_._2).flatMap(_.mainchainBlockReferenceHashes)
    assertEquals("All mainchain references are expected to be added", mainchainReferences.size + genesisBlockMainchainHeight - 1, allAtOnceChain.heightOfMc)
    assertEquals("All sidechain references are expected to be added", usedIds.size, filledByBestBlock.height)
  }

  @Test
  def verifyAddedData(): Unit = {
    setSeed(testSeed)

    val chainHeight: Int = 10
    val data = generateDataSequenceWithGenesisBlock(chainHeight)
    val parent = data.flatMap(_._3).head
    val blockInfoData = data.map(b => (b._1, b._2))
    val chain = ActiveChain(data.map{case (id, sbInfo, _) => (id, sbInfo)}(breakOut), parent, genesisBlockMainchainHeight)

    val mainChainReferenceHashToModifierId = {
      for {
        (modifierId, sidechainBlock) <- blockInfoData
        mainchainReferenceHash <- sidechainBlock.mainchainBlockReferenceHashes
      } yield (mainchainReferenceHash, modifierId)
      }.toSet

    val mainchainBlockByReferenceHashToBlockId =
      mainChainReferenceHashToModifierId.map{case(mainchainReferenceHash, _) => (mainchainReferenceHash, chain.idByMcId(mainchainReferenceHash).get)}

    val diff = mainChainReferenceHashToModifierId.diff(mainchainBlockByReferenceHashToBlockId)
    assertTrue(s"Mainchain block id by mainchainBlockReferenceHash is failed: ${diff}", diff.isEmpty)

    val mainChainBlockReferenceHashToMainchainHeight =
      blockInfoData.flatMap(_._2.mainchainBlockReferenceHashes).zipWithIndex.map{case(reference, index) => (reference, index + genesisBlockMainchainHeight)} //height starts from 1

    val failedHeights =
      mainChainBlockReferenceHashToMainchainHeight
        .map{case(hash, height) => (hash, height, chain.mcHeightByMcId(hash).get)}
        .filter{case(_, expectedHeight, realHeight) => expectedHeight != realHeight}

    val assertMessage: String = if (failedHeights.nonEmpty) {
      s"Mainchain calculation had been failed, i.e. for hash ${failedHeights.head._1} expected ${failedHeights.head._2}, but got ${failedHeights.head._3}"
    } else {""}

    assertTrue(assertMessage, failedHeights.isEmpty)

    assertEquals("ActiveChain expected to have different height", chainHeight, chain.height)
    assertEquals("ActiveChain expected to have different tip", blockInfoData.last._1, chain.bestId.get)
    assertEquals("ActiveChain expected to have different tipInfo", blockInfoData.last._2.parentId, chain.bestScBlockInfo.get.parentId)

    assertTrue("ActiveChain expected not to find height of nonexistent modifier", chain.heightById(getRandomModifier()).isEmpty)
    assertEquals("ActiveChain expected not to find height of first(genesis) modifier", 1, chain.heightById(blockInfoData.head._1).get)
    assertEquals("ActiveChain expected not to find height of 3rd modifier", 3, chain.heightById(blockInfoData(2)._1).get)
    assertEquals("ActiveChain expected not to find height of tip modifier", chainHeight, chain.heightById(blockInfoData.last._1).get)

    assertFalse("ActiveChain expected not to contain nonexistent modifier", chain.contains(getRandomModifier()))
    assertTrue("ActiveChain expected to contain first(genesis) modifier", chain.contains(blockInfoData.head._1))
    assertTrue("ActiveChain expected to contain 3rd modifier", chain.contains(blockInfoData(2)._1))
    assertTrue("ActiveChain expected to contain tip modifier", chain.contains(blockInfoData.last._1))

    assertTrue("ActiveChain expected not to find nonexistent modifier", chain.blockInfoById(getRandomModifier()).isEmpty)
    assertTrue("ActiveChain expected to find first(genesis) modifier", chain.blockInfoById(blockInfoData.head._1).isDefined)
    assertTrue("ActiveChain expected to find 3rd modifier", chain.blockInfoById(blockInfoData(2)._1).isDefined)
    assertTrue("ActiveChain expected to find tip modifier", chain.blockInfoById(blockInfoData.last._1).isDefined)

    assertTrue("ActiveChain expected not to find modifier for inconsistent height", chain.blockInfoByHeight(chainHeight + 1).isEmpty)
    assertTrue("ActiveChain expected to find modifier for height 1", chain.blockInfoByHeight(1).isDefined)
    assertTrue("ActiveChain expected to find modifier for chain current height", chain.blockInfoByHeight(chainHeight).isDefined)

    assertTrue("ActiveChain expected not to find modifier id for inconsistent height", chain.idByHeight(0).isEmpty)
    assertTrue("ActiveChain expected not to find modifier id for inconsistent height", chain.idByHeight(chainHeight + 1).isEmpty)
    assertTrue("ActiveChain expected to find modifier id for height 1", chain.idByHeight(1).isDefined)
    assertTrue("ActiveChain expected to find modifier id for chain current height", chain.idByHeight(chainHeight).isDefined)

    assertTrue("ActiveChain expected to return empty chain from nonexistent modifier", chain.chainAfter(getRandomModifier()).isEmpty)

    var chainAfter: Seq[ModifierId] = chain.chainAfter(blockInfoData.head._1)
    assertEquals("ActiveChain chainAfter expected to return chain with different height for first(genesis) modifier",
      blockInfoData.size, chainAfter.size)
    for(i <- chainAfter.indices)
      assertEquals("ActiveChain chainAfter item at index %d is different".format(i), blockInfoData(i)._1, chainAfter(i))


    val startingIndex = 3
    chainAfter = chain.chainAfter(blockInfoData(startingIndex)._1)
    assertEquals("ActiveChain chainAfter expected to return chain with different height for 4th modifier",
      chainHeight - startingIndex, chainAfter.size)
    for(i <- chainAfter.indices)
      assertEquals("ActiveChain chainAfter item at index %d is different".format(i), blockInfoData(i + startingIndex)._1, chainAfter(i))


    chainAfter = chain.chainAfter(blockInfoData.last._1)
    assertEquals("ActiveChain chainAfter expected to return chain with size 1 for tip modifier", 1, chainAfter.size)
    assertEquals("ActiveChain chainAfter item at index 0 is different", blockInfoData.last._1, chainAfter.head)
  }



  @Test
  def tipUpdate(): Unit = {
    setSeed(testSeed)

    // Update a tip of empty ActiveChain
    val chain: ActiveChain = ActiveChain(genesisBlockMainchainHeight)
    val mainchainData: Seq[MainchainBlockReferenceId] = Seq()

    val (firstId: ModifierId, firstData: SidechainBlockInfo, firstMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(getRandomModifier(), Seq(generateMainchainBlockReference()))
    addNewBestBlockIsSuccessful(chain, firstId, firstData, firstMainchainParent)
    val mainchainDataAfterFirst = mainchainData ++ firstData.mainchainBlockReferenceHashes

    checkElementIsPresent(chain, firstId, firstData, 1, 0, mainchainDataAfterFirst)
    checkElementIsBest(chain, firstId, firstData, 1)
    assertEquals("ChainFrom the beginning should contain just a tip", Seq(firstId), chain.chainAfter(firstId))

    // Try to add the same element second time
    addNewBestBlockShallBeFailed(chain, firstId, firstData, firstMainchainParent)
    checkElementIsBest(chain, firstId, firstData, 1)
    assertEquals("ChainFrom the beginning should contain just a tip", Seq(firstId), chain.chainAfter(firstId))

    // Add second element
    val (secondId: ModifierId, secondData: SidechainBlockInfo, secondMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(firstId)
    addNewBestBlockIsSuccessful(chain, secondId, secondData, secondMainchainParent)
    val mainchainDataAfterSecond = mainchainDataAfterFirst ++ secondData.mainchainBlockReferenceHashes

    checkElementIsPresent(chain, secondId, secondData, 2, mainchainDataAfterFirst.size, mainchainDataAfterSecond)
    checkElementIsBest(chain, secondId, secondData, 2)
    assertEquals("ChainFrom the beginning should contain all ids", Seq(firstId, secondId), chain.chainAfter(firstId))
    checkElementIsNotBest(chain, firstId, firstData, 1)

    // Add third element
    val (thirdId: ModifierId, thirdData: SidechainBlockInfo, thirdMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(secondId)
    addNewBestBlockIsSuccessful(chain, thirdId, thirdData, thirdMainchainParent)
    val mainchainDataAfterThird = mainchainDataAfterSecond ++ thirdData.mainchainBlockReferenceHashes

    checkElementIsPresent(chain, thirdId, thirdData, 3, mainchainDataAfterSecond.size, mainchainDataAfterThird)
    checkElementIsBest(chain, thirdId, thirdData, 3)
    assertEquals("ChainFrom the beginning should contain all ids", Seq(firstId, secondId, thirdId), chain.chainAfter(firstId))
    checkElementIsNotBest(chain, secondId, secondData, 2)

    // Add fourth element
    val (fourthId: ModifierId, fourthData: SidechainBlockInfo, fourthMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(thirdId)
    addNewBestBlockIsSuccessful(chain, fourthId, fourthData, fourthMainchainParent)
    val mainchainDataAfterFourth = mainchainDataAfterThird ++ fourthData.mainchainBlockReferenceHashes

    checkElementIsPresent(chain, fourthId, fourthData, 4, mainchainDataAfterThird.size, mainchainDataAfterFourth)
    assertEquals("ChainFrom the beginning should contain all ids", Seq(firstId, secondId, thirdId, fourthId), chain.chainAfter(firstId))

    //replace last element
    val (otherFourthId: ModifierId, otherFourthData: SidechainBlockInfo, otherFourthMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(thirdId)
    addNewBestBlockIsSuccessful(chain, otherFourthId, otherFourthData, otherFourthMainchainParent)
    val mainchainDataAfterOtherFourth = mainchainDataAfterThird ++ otherFourthData.mainchainBlockReferenceHashes

    checkElementIsPresent(chain, otherFourthId, otherFourthData, 4, mainchainDataAfterThird.size, mainchainDataAfterOtherFourth)
    checkElementIsNotPresent(chain, fourthId, fourthData, 4)
    assertEquals("ChainFrom the beginning should contain all ids", Seq(firstId, secondId, thirdId, otherFourthId), chain.chainAfter(firstId))


    // do fork on the second element and add element
    val (otherThirdId: ModifierId, otherThirdData: SidechainBlockInfo, otherThirdMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(secondId)
    addNewBestBlockIsSuccessful(chain, otherThirdId, otherThirdData, otherThirdMainchainParent)
    val mainchainDataAfterOtherThird = mainchainDataAfterSecond ++ otherThirdData.mainchainBlockReferenceHashes

    checkElementIsPresent(chain, otherThirdId, otherThirdData, 3, mainchainDataAfterSecond.size, mainchainDataAfterOtherThird)
    checkElementIsBest(chain, otherThirdId, otherThirdData, 3)
    checkElementIsNotPresent(chain, otherFourthId, otherFourthData, 4)
    assertEquals("ChainFrom the beginning should contain all ids", Seq(firstId, secondId, otherThirdId), chain.chainAfter(firstId))

    val (afterThirdId: ModifierId, afterThirdData: SidechainBlockInfo, afterThirdMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(otherThirdId)
    addNewBestBlockIsSuccessful(chain, afterThirdId, afterThirdData, afterThirdMainchainParent)
    val mainchainDataAfterAfterThird = mainchainDataAfterOtherThird ++ afterThirdData.mainchainBlockReferenceHashes

    checkElementIsPresent(chain, afterThirdId, afterThirdData, 4, mainchainDataAfterOtherThird.size, mainchainDataAfterAfterThird)
    checkElementIsBest(chain, afterThirdId, afterThirdData, 4)
    assertEquals("ChainFrom the beginning should contain all ids", Seq(firstId, secondId, otherThirdId, afterThirdId), chain.chainAfter(firstId))

    // try to add unconnected element
    val (unconnectedId: ModifierId, unconnectedData: SidechainBlockInfo, unconnectedMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(getRandomModifier())
    addNewBestBlockShallBeFailed(chain, unconnectedId, unconnectedData, unconnectedMainchainParent)
    checkElementIsPresent(chain, afterThirdId, afterThirdData, 4, mainchainDataAfterOtherThird.size, mainchainDataAfterAfterThird)
    checkElementIsBest(chain, afterThirdId, afterThirdData, 4)
    assertEquals("ChainFrom the beginning should contain all ids", Seq(firstId, secondId, otherThirdId, afterThirdId), chain.chainAfter(firstId))
  }

  @Test
  def genesisBlockWithoutMainchaiReferences(): Unit = {
    setSeed(testSeed)

    val chain: ActiveChain = ActiveChain(genesisBlockMainchainHeight)
    val mainchainData: Seq[MainchainBlockReferenceId] = Seq()

    // Add first element with no mainchain references
    val (firstId: ModifierId, firstData: SidechainBlockInfo, mainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParentNoMainchainReferences(getRandomModifier())

    addNewBestBlockShallBeFailed(chain, firstId, firstData, mainchainParent)
  }

  @Test
  def genesisBlockWithoutMainchainReferencesAndParent(): Unit = {
    setSeed(testSeed)

    val chain: ActiveChain = ActiveChain(genesisBlockMainchainHeight)

    // Add first element with no mainchain references and no parent
    val (firstId: ModifierId, firstData: SidechainBlockInfo, _: Option[MainchainBlockReferenceId]) = getNewDataForParentNoMainchainReferences(getRandomModifier())
    addNewBestBlockShallBeFailed(chain, firstId, firstData, None)
  }

  @Test
  def addBlockWithNoMainchainReferences(): Unit = {
    setSeed(testSeed + 1)

    // Update a tip of empty ActiveChain
    val chain: ActiveChain = ActiveChain(genesisBlockMainchainHeight)
    val mainchainData: Seq[MainchainBlockReferenceId] = Seq()

    val (firstId: ModifierId, firstData: SidechainBlockInfo, mainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(getRandomModifier(), Seq(generateMainchainBlockReference()))
    addNewBestBlockIsSuccessful(chain, firstId, firstData, mainchainParent)
    val mainchainDataAfterFirst = mainchainData ++ firstData.mainchainBlockReferenceHashes

    checkElementIsPresent(chain, firstId, firstData, 1, 0, mainchainDataAfterFirst)
    checkElementIsBest(chain, firstId, firstData, 1)
    assertEquals("ChainFrom the beginning should contain just a tip", Seq(firstId), chain.chainAfter(firstId))

    // Add second element
    val (secondId: ModifierId, secondData: SidechainBlockInfo, secondMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParent(firstId, Seq(generateMainchainBlockReference()))
    addNewBestBlockIsSuccessful(chain, secondId, secondData, secondMainchainParent)
    checkElementIsBest(chain, secondId, secondData, 2)
    assertEquals("ChainFrom the beginning should contain all ids", Seq(firstId, secondId), chain.chainAfter(firstId))
    val mainchainDataAfterSecond = mainchainDataAfterFirst ++ secondData.mainchainBlockReferenceHashes

    // Add third element
    val (thirdId: ModifierId, thirdData: SidechainBlockInfo, thirdMainchainParent: Option[MainchainBlockReferenceId]) = getNewDataForParentNoMainchainReferences(secondId)
    addNewBestBlockIsSuccessful(chain, thirdId, thirdData, thirdMainchainParent)
    val mainchainDataAfterThird = mainchainDataAfterSecond ++ thirdData.mainchainBlockReferenceHashes
    checkElementIsPresent(chain, thirdId, thirdData, 3, mainchainDataAfterSecond.size, mainchainDataAfterThird)
    checkElementIsBest(chain, thirdId, thirdData, 3)
    assertEquals("ChainFrom the beginning should contain all ids", Seq(firstId, secondId, thirdId), chain.chainAfter(firstId))
  }

  //@Test
  def checkTests(): Unit = {
    (1 to 100000).foreach{_ =>
      val newSeed = System.nanoTime()
      println(s"Start tests for seed ${newSeed}")

      testSeed = newSeed

      tipUpdate()
      checkEmptyChain()
      genesisBlockWithoutMainchaiReferences()
      verifyAddedData()
      checkFilledByDataChain()
    }
  }
}
