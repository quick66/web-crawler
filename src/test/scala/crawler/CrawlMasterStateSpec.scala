package crawler

import java.net.URL

import testutil.UnitSpec

class CrawlMasterStateSpec extends UnitSpec {

    "Crawler Master State" should "clean queue top from processed urls when dequeueing" in {
        val correctHead = new URL("http://example.com/page")
        val processedHeads = Seq("http://foo.bar/page", "http://foo.bar/page/a").map(new URL(_))
        val processedTail = new URL("http://foo.bar/page/b")
        val processed = (processedHeads :+ processedTail).toSet
        val tail = Seq(new URL("http://example.com/page/a")) :+ processedTail

        val state = CrawlMasterState(
            queue = (processedHeads :+ correctHead) ++ tail,
            processed = processed
        )

        val correctState = CrawlMasterState(
            queue = tail,
            processed = processed
        )

        val (headOpt, dequeued) = state.dequeueUrl

        assert(headOpt.get == correctHead)
        assert(dequeued == correctState)
    }

    "Crawler Master State" should "not fail and not change on empty queue" in {

        val state = CrawlMasterState()

        val (headOpt, dequeued) = state.dequeueUrl

        assert(headOpt.isEmpty)
        assert(dequeued == state)
    }

}
