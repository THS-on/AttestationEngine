#!/bin/sh
#Copyright 2021 Nokia
#Licensed under the BSD 3-Clause Clear License.
#SPDX-License-Identifier: BSD-3-Clear

docker run -p 8530:8530 --device=/dev/tpm0 nut10
