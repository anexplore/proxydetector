# -*- coding: utf-8 -*-
"""
Return Request's HTTP Headers
"""
import sys
import time

import tornado
import tornado.httpserver
import tornado.web
import tornado.gen as gen


def log(string):
    try:
        sys.stdout.write('%s:%s\n' % (time.strftime('%Y-%m-%d %H:%M:%S', time.localtime()),
                                      string.encode('UTF-8')))
        sys.stdout.flush()
    except:
        pass


class DefaultHandler(tornado.web.RequestHandler):
    @gen.coroutine
    def get(self):
        self.set_status(200, 'OK')
        self.write('hello bad body')
        self.finish()

    @gen.coroutine
    def post(self):
        self.get()


class ProxyJudgeHandler(tornado.web.RequestHandler):
    """Return headers
    """

    @gen.coroutine
    def get(self):
        self.set_status(200, 'OK')
        response = []
        for key, value in self.request.headers.iteritems():
            response.append('HTTP_%s = %s' % (key.replace('-', '_').upper(), value))
        response.append('REMOTE_ADDR = %s' % self.request.remote_ip)
        html = '<html><head><title>proxyjudge</title></head><body>\n<pre>\n' + '\r\n'.join(response)\
               + '\n</pre></body></html>'
        self.write(html)
        self.finish()

    @gen.coroutine
    def post(self):
        self.get()


class Server(object):

    def __init__(self, address=None, port=8000):
        if not address:
            address = '0.0.0.0'
        self.address = address
        self.port = port
        self.http_server = None

    def start(self):
        settings = dict(
        )
        application = tornado.web.Application([
            (r"/", DefaultHandler),
            (r"/proxyjudge", ProxyJudgeHandler)],
            **settings)
        self.http_server = tornado.httpserver.HTTPServer(application) 
        self.http_server.listen(self.port)
        tornado.ioloop.IOLoop.current().start()

    def shutdown(self):
        if self.http_server is not None:
            self.http_server.stop()
            tornado.ioloop.IOLoop.current().stop()
        log('server stops')


if __name__ == '__main__':
    port = 8000
    if len(sys.argv) > 1:
        port = int(sys.argv[1])
    server = Server(port=port)
    import signal

    def sig_handler(signum, frame):
        print 'get sig:%d' % signum
        receiver.shutdown()

    signal.signal(signal.SIGTERM, sig_handler)
    server.start()
