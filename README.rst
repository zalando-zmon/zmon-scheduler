ZMON source code on GitHub is no longer in active development. Zalando will no longer actively review issues or merge pull-requests.

ZMON is still being used at Zalando and serves us well for many purposes. We are now deeper into our observability journey and understand better that we need other telemetry sources and tools to elevate our understanding of the systems we operate. We support the `OpenTelemetry <https://opentelemetry.io>`_ initiative and recommended others starting their journey to begin there.

If members of the community are interested in continuing developing ZMON, consider forking it. Please review the licence before you do.

==============
ZMON Scheduler
==============

.. image:: https://travis-ci.org/zalando-zmon/zmon-scheduler.svg?branch=master
   :target: https://travis-ci.org/zalando-zmon/zmon-scheduler
   :alt: Build Status

.. image:: https://codecov.io/github/zalando-zmon/zmon-scheduler/coverage.svg?branch=master
   :target: https://codecov.io/github/zalando-zmon/zmon-scheduler
   :alt: Code Coverage

The ZMON Scheduler is responsible for keeping track of all existing entities, checks and alerts and scheduling checks in time for applicable entities, which are then executed by the `ZMON Worker`_.

Running Unit Tests
==================

.. code-block:: bash

    $ ./mvnw clean test

Running Locally
===============

.. code-block:: bash

    $ ./mvnw clean install
    $ java -jar target/zmon-scheduler-1.0-SNAPSHOT.jar

Building the Docker Image
=========================

.. code-block:: bash

    $ ./mvnw clean package
    $ docker build -t zmon-scheduler .


See also the `ZMON Documentation`_.

.. _main ZMON repository: https://github.com/zalando/zmon
.. _ZMON Documentation: https://docs.zmon.io/
.. _ZMON Worker: https://github.com/zalando-zmon/zmon-worker
