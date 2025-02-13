package com.datasophon.api.master;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.SubmitActiveTaskNodeCommand;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubmitTaskNodeActor extends UntypedActor {
    private static final Logger logger = LoggerFactory.getLogger(SubmitTaskNodeActor.class);


    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        logger.info("service command actor restart because {}", reason.getMessage());
        super.preRestart(reason, message);
    }

    @Override
    public void onReceive(Object message) throws Throwable {

        if (message instanceof SubmitActiveTaskNodeCommand) {
            SubmitActiveTaskNodeCommand submitActiveTaskNodeCommand = (SubmitActiveTaskNodeCommand) message;
            DAGGraph<String, ServiceNode, String> dag = submitActiveTaskNodeCommand.getDag();
            Map<String, ServiceExecuteState> activeTaskList = submitActiveTaskNodeCommand.getActiveTaskList();
            Map<String, String> errorTaskList = submitActiveTaskNodeCommand.getErrorTaskList();
            Map<String, String> readyToSubmitTaskList = submitActiveTaskNodeCommand.getReadyToSubmitTaskList();
            Map<String, String> completeTaskList = submitActiveTaskNodeCommand.getCompleteTaskList();
            //遍历dag执行服务指令
            if (readyToSubmitTaskList.size() > 0) {
                for (String node : readyToSubmitTaskList.keySet()) {
                    //判断前置节点是否完成，若未完成，则取消执行并在待运行列表中移除该节点
                    Set<String> previousNodes = dag.getPreviousNodes(node);
                    for (String previousNode : previousNodes) {
                        if (errorTaskList.containsKey(previousNode)) {
                            readyToSubmitTaskList.remove(node);
                            continue;
                        }
                    }
                    if (activeTaskList.containsKey(node)) {
                        continue;
                    }
                    if (completeTaskList.containsKey(node)) {
                        continue;
                    }
                    ServiceNode serviceNode = dag.getNode(node);
                    List<ServiceRoleInfo> masterRoles = serviceNode.getMasterRoles();

                    activeTaskList.put(node, ServiceExecuteState.RUNNING);
                    ActorRef serviceActor = ActorUtils.getLocalActor(ServiceActor.class, submitActiveTaskNodeCommand.getClusterCode() + "-serviceActor-" + node);
                    if (masterRoles.size() > 0) {
                        logger.info("start to submit {} master roles", node);
                        ProcessUtils.buildExecuteServiceRoleCommand(submitActiveTaskNodeCommand.getClusterId(), submitActiveTaskNodeCommand.getCommandType(),submitActiveTaskNodeCommand.getClusterCode(), dag, activeTaskList, errorTaskList, readyToSubmitTaskList, completeTaskList, node, masterRoles, serviceActor, ServiceRoleType.MASTER);
                    } else if (serviceNode.getElseRoles().size() > 0) {
                        logger.info("{} does not has master roles , start to submit worker or client roles", node);
                        ProcessUtils.buildExecuteServiceRoleCommand(submitActiveTaskNodeCommand.getClusterId(), submitActiveTaskNodeCommand.getCommandType(), submitActiveTaskNodeCommand.getClusterCode(),dag, activeTaskList, errorTaskList, readyToSubmitTaskList, completeTaskList, node, serviceNode.getElseRoles(), serviceActor, ServiceRoleType.WORKER);
                    } else {
                        continue;
                    }
                }
            }
        }
    }

}
