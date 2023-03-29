# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
from iotdb.mlnode.constant import TSStatusCode
from iotdb.mlnode.exception import BadNodeUrlError
from iotdb.mlnode.log import logger
from iotdb.mlnode.parser import ConfigManager
from iotdb.thrift.common.ttypes import TEndPoint, TSStatus
from iotdb.thrift.mlnode.ttypes import TCreateTrainingTaskReq


def parse_endpoint_url(endpoint_url: str) -> TEndPoint:
    """ Parse TEndPoint from a given endpoint url.
    Args:
        endpoint_url: an endpoint url, format: ip:port
    Returns:
        TEndPoint
    Raises:
        BadNodeUrlError
    """
    split = endpoint_url.split(":")
    if len(split) != 2:
        logger.warning("Illegal endpoint url format: {}".format(endpoint_url))
        raise BadNodeUrlError(endpoint_url)

    ip = split[0]
    try:
        port = int(split[1])
        result = TEndPoint(ip, port)
        return result
    except ValueError as e:
        logger.warning("Illegal endpoint url format: {} ({})".format(endpoint_url, e))
        raise BadNodeUrlError(endpoint_url)


def get_status(status_code: TSStatusCode, message: str) -> TSStatus:
    status = TSStatus(status_code.get_status_code())
    status.message = message
    return status


def verify_success(status: TSStatus, err_msg: str) -> None:
    if status.code != TSStatusCode.SUCCESS_STATUS:
        logger.warn(err_msg + ", error status is ", status)
        raise RuntimeError(str(status.code) + ": " + status.message)


def parse_training_request(req: TCreateTrainingTaskReq):
    """
    Parse TCreateTrainingTaskReq with given yaml template
    Args:
        req: TCreateTrainingTaskReq
    Returns:
        data_conf: configurations related to data
        model_conf: configurations related to model
        task_conf: configurations related to task
    """
    config = req.modelConfigs
    config.update(model_id=req.modelId)
    config.update(tuning=req.isAuto)
    config.update(query_expressions=req.queryExpressions)
    config.update(query_filter=req.queryFilter)
    config_manager = ConfigManager(config)
    return config_manager.get_configs()
