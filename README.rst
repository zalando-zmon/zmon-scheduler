=================
ZMON Scheduler NG
=================

.. image:: https://travis-ci.org/zalando/zmon-scheduler-ng.svg?branch=master
   :target: https://travis-ci.org/zalando/zmon-scheduler-ng
   :alt: Build Status

This is the replacement for our current Python "zmon-scheduler" as part of the ZMON infrastructure.

This greatly improves scheduling throughput, better ontime scheduling not affected by background tasks as refreshes and makes real instant evaluation possible via ZMON frontend.

Running Unit Tests
==================

.. code-block:: bash

    $ ./mvnw clean test

