#Copyright 2021 Nokia
#Licensed under the BSD 3-Clause Clear License.
#SPDX-License-Identifier: BSD-3-Clear

import a10.structures.constants
import a10.structures.returncode


class A10ProtocolBase:
    NAME = "A10BaseProtocol"

    def __init__(self, endpoint, policyintent, policyparameters, callparameters):
        self.endpoint = endpoint
        self.policyintent = policyintent
        self.policyparameters = policyparameters
        self.callparameters = callparameters

    def exec(self):
        return a10.structures.returncode.ReturnCode(
            a10.structures.constants.SUCCESS,
            {"message": "a10protocolBase test return from exec() call"},
        )
