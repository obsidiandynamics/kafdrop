package kafdrop.model;

import org.apache.kafka.common.Node;

import java.util.Collection;

public record ClusterDescriptionVO(Collection<Node> nodes,
                                   Node controller,
                                   String clusterId) {
}
