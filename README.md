# web-crawler

A very simple crawler of web-pages with domain filter written in Scala with power of Akka and with a small pinch of 
Jsoup parsing.

## Configuration

Application accepts configuration in HOCON format. Defined options are:
* `app.request-state-timeout` - timeout for asking master state.
* `crawler.master.dequeue-next-url-interval` - interval between attempts of dequeueing the crawling queue.
* `crawler.worker.count` - size of worker's pool.
* `crawler.storage.root-path` - folder where documents should be stored.

## API

* `GET /status` - shows info about crawling master state in JSON format. Example of response:
~~~~json
{
  "paused": false,
  "enqueued": 1465,
  "processed": 688
}
~~~~
where `paused` means "is top items from the crawling queue currently processed", `enqueued` equals the size of 
the crawling queue and `processed` equals the count of processed documents.

* `POST /page` - adds new URL to the crawling queue. Request body is JSON, for example:
~~~~json
{
  "url": "https://akka.io"
}
~~~~

* `POST /pause` - Pauses processing of the crawling queue (new items could be added). Request body is empty.

* `POST /resume` - Resumes processing of the crawling queue. Request body is empty.

* `GET /domains` - lists domains, from which pages could be processed. Example of response:
~~~~json
{
  "domains": [
    "akka.io",
    "scala-lang.org"
  ]
}
~~~~

* `POST /domains` - sets domains, from which pages could be processed. Request body format is similar to response body 
of previous API request.
