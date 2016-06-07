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

    $ sudo pip3 install scm-source
    $ scm-source -f target/scm-source.json
    $ docker build -t zmon-scheduler .


See also the `ZMON Documentation`_.

.. _main ZMON repository: https://github.com/zalando/zmon
.. _ZMON Documentation: https://docs.zmon.io/
.. _ZMON Worker: https://github.com/zalando-zmon/zmon-worker
