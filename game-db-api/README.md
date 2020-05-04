### fastjgame项目 数据库层api

Q: 为什么在写数组的过程中不需要writeName，而写Map的时候需要?   
A: 如果把数组的每一个元素看作一个成员字段的值，那么索引就是成员变量名字。  
而map则不同，map的key类型是任意的，key不一定能转换成字符串(或不应该)，因此key不一定能作为成员变量的名字。  
如果map的key可以简单的转换为字符串，那么map就相当于一个简单对象，按照简单的写对象方式写即可。  
如果不能，我们则需要将key和value都当作成员变量，需要寻求别的作为成员变量名字。eg: 将key-value展开，当作数组，或k1,v1,k2,v2这种方式作为变量名字。

Q: 如何写入Map？
A: 对于key能简单转换为字符串的map，可以直接当做普通对象写，把key作为name，比如key为{@link Number}。
```
     writer.writeStartObject("mapFieldName");
     for(Map.Entry<Integer,V> entry: map) {
        writer.writeName(entry.getKey().toString());
        // value写在这里
        writeValue(writer, entry.getValue());
     }
     writer.writeEndObject();
```
对于key是其它对象的map，每一个key和value都当做内嵌对象(文档)写，因此可以当做array或对象写：
 ```
    // 方式1：将key和value都理解为成员变量，用 k1,v1,k2,v2的方式写入 - 这种方式有很好的可读性，缺陷是字符串拼接，不过一般不是大问题
    // 可以缓存一定长度的key-value名字，比如缓存128个，由统一的地方负责makeKeyName(index) makeValueName(index)，小于128的直接从缓存数组取。
    int index = 0;
    writer.writeStartObject("mapFieldName");
    for(Map.Entry<K,V> entry: map) {
        writer.writeName("k" + index);
        // key写这里
        writeKey(writer, entry.getKey());
  
        writer.writeName("v" + index);
        // value写在这里
        writeValue(writer, entry.getValue());
        
        index++;
     }
     writer.writeEndObject();
    }
    
    // 方式2: 将key-value展开，当作数组，紧挨着的两个元素为一个键值对 - 这种方式可以避免字符串拼接，减少消耗，不过会降低表达力。
 ```
 
 ### json/bson/protoBuffer 简单比较
 json字段的构成： name + value  
 protoBuffer字段的构成: number + type + value  
 bson字段的构成: name + type + value  
 
 Q: 序列化name的好处和缺陷？  
 A: 好处是可以增加可读性，缺陷是会导致传输量的增加，而且增加非常多。  
 
 Q: 序列化type的好处和缺陷？  
 A: 如果不序列化type，将无法精确解析。json的number就是一个问题，整形和浮点数之间无法区分。缺陷是也会导致传输量增加，但是type增加的传输量较少，一般一个字节就可以表述类型信息。  
 
 Q: 有没有可能不需要序列化name和type?  
 A: 如果一个类型定义是精确的，不使用任何的动态特性，那么就是可能的。如果使用多态特性，那么字段本身将无法告诉你精确信息，就必须要存储运行时的类型信息。
 
 Q: 入库与序列化的不同？  
 A: 在入库时，必定需要name+type+value，既要可读，又要能精确解析。如果仅仅用作服务器之间的通信，也就是序列化过程，只要读写顺序一致，可以不需要name，
 但是类型信息仍然要存储，否则无法精确解析。或者可以参考protoBuffer使用number和type标记。
 
 Q: 为什么要定义binary类型?  
 A: byte[]可能蕴含丰富的信息，我们需要让它带有一定的自解释性，否则拿到一个byte[]将无法操作。
 
 总结如下：
 1. bson格式存储的内容是最丰富的，因此序列化之后的长度是最多的。  
 2. protoBuffer属于另辟蹊径类型，使用字段的number代替name，从而大大减少传输量。  
 
 ### input/output最小元数据类型集
 1. 8种基本类型
 2. String
 3. bytes
 4. Message (protoBuf)
 
 Q: 为什么要把Message归入最小数据集？
 A: Message序列化占比非常高，我们必须想办法减少它的开销，而它的优化必须要深入到最终写入的时候。