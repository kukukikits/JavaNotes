# 配置h2内存数据库，支持geometry数据类型

## pom.xml
```xml

    <repositories>
        <repository>
            <id>osgeo</id>
            <name>OSGeo Release Repository</name>
            <url>https://repo.osgeo.org/repository/release/</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <releases>
                <enabled>true</enabled>
            </releases>
        </repository>
        <repository>
            <id>osgeo-snapshot</id>
            <name>OSGeo Snapshot Repository</name>
            <url>https://repo.osgeo.org/repository/snapshot/</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
            <releases>
                <enabled>false</enabled>
            </releases>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.hibernate</groupId>
            <artifactId>hibernate-spatial</artifactId>
            <version>5.3.7.Final</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/org.opengeo/geodb -->
        <dependency>
            <groupId>org.opengeo</groupId>
            <artifactId>geodb</artifactId>
            <version>0.9</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.bedatadriven</groupId>
            <artifactId>jackson-datatype-jts</artifactId>
            <version>2.4</version>
        </dependency>
        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-main</artifactId>
            <version>20.1</version>
        </dependency>
        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-api</artifactId>
            <version>20.1</version>
        </dependency>
    </dependencies>
```

## com.vividsolutions.jts-core和org.locationtech.jts-core两个包冲突
这两个包都是jts规范的核心实现，里面的类、方法都差不多，但是pom依赖中使用的包不一样

| dependency           | jts-core依赖   |
| -------------------- | -------------- |
| hibernate-spatial    | vividsolutions |
| geodb                | locationtech   |
| jackson-datatype-jts | vividsolutions |
| gt-main              | locationtech   |
| gt-api               | locationtech   |

由此可见，hibernate-spatial使用jts依赖的和geotools工具使用的jts依赖不一样，所以还要注意，从数据库中查出的空间数据如果要使用Geotools操作，还需要提前转换类型。

## 自定义数据库方言
```java
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.dialect.h2geodb.GeoDBDialect;
import org.hibernate.spatial.dialect.h2geodb.GeoDBGeometryTypeDescriptor;
import org.hibernate.spatial.dialect.h2geodb.GeoDbWkb;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;

/**
 * 使用geodb来支持空间数据时，由于geodb返回的空间数据模型是org.locationtech.jts这个包中定义的模型，
 * 而本项目使用的是com.vividsolutions.jts定义的几何模型。所以当查询时数据库返回一个org.locationtech.jts.geom.Geometry
 * 对象时，需要把这个对象转换为com.vividsolutions.jts.geo.Geometry对象。这个自定义的类就是做这个转换用的。
 * <br/>
 * 如果项目里使用的JTS包是org.locationtech.jts，那么应该也不需要这个自定义的转换器了
 * <br/>
 * 使用时设置如下属性：
 * #替换package-name
 * spring.jpa.properties.hibernate.dialect=package-name.CustomGeoDBDialect
 *
 * 注意：geodb在项目中只能用于测试
 */
public class CustomGeoDBDialect extends GeoDBDialect {
    private static final InternalGeoDBGeometryTypeDescriptor INTERNAL_TYPEDESC_INSTANCE = new InternalGeoDBGeometryTypeDescriptor();

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes( typeContributions, serviceRegistry );
        typeContributions.contributeType( new JTSGeometryType( INTERNAL_TYPEDESC_INSTANCE ) );
    }
}

class InternalGeoDBGeometryTypeDescriptor extends GeoDBGeometryTypeDescriptor {
    private static final Logger log = Logger.getLogger(InternalGeoDBGeometryTypeDescriptor.class.getName());

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicExtractor<X>( javaTypeDescriptor, this ) {
            @Override
            protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
                Object obj = convertLocationtechToVivid( rs.getObject( name ) );
                return getJavaDescriptor().wrap( GeoDbWkb.from( obj ), options );
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                Object obj = convertLocationtechToVivid( statement.getObject( index ) );
                return getJavaDescriptor().wrap( GeoDbWkb.from( obj ), options );
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                Object obj = convertLocationtechToVivid( statement.getObject( name ) );
                return getJavaDescriptor().wrap( GeoDbWkb.from( obj ), options );
            }

            //Conversion from org.locationtech to com.vividsolutions
            private Object convertLocationtechToVivid(Object geom){
                try{
                    if (geom instanceof Geometry){
                        Geometry geo = (Geometry) geom;
                        String write = new WKTWriter().write(geo);
                        com.vividsolutions.jts.geom.Geometry read = new WKTReader().read(write);
                        read.setSRID(geo.getSRID());
                        return read;
                    }
                }catch (ParseException e){
                    throw new RuntimeException(e);
                }
                return geom;
            }
        };
    }
}

```

## application.properties配置文件配置

```properties
# mysql使用geometry类型数据配置
spring.jpa.properties.hibernate.dialect=org.hibernate.spatial.dialect.mysql.MySQL5InnoDBSpatialDialect
spring.jpa.database-platform=org.hibernate.spatial.dialect.mysql.MySQLSpatialDialect
spring.jpa.properties.hibernate.dialect.storage_engine=innodb
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# h2使用geometry类型数据配置
spring.jpa.properties.hibernate.dialect=package-name..CustomGeoDBDialect
spring.jpa.properties.hibernate.jdbc.use_get_generated_keys=false
```

## h2和mysql建表

h2数据库
```sql
-- 初始化org.opengeo
CREATE ALIAS InitGeoDB for "geodb.GeoDB.InitGeoDB";
CALL InitGeoDB();
create table geometry_table (
    id INTEGER AUTO_INCREMENT,
    geometry BLOB
);

insert into geometry_table (geometry) values (ST_GeomFromText('POINT (120 30)', 4326));
```

mysql数据库
```sql
CREATE TABLE `geometry_table` (
  `id` BIGINT AUTO_INCREMENT,
  `geometry` geometry
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
insert into geometry_table (geometry) values (ST_GeomFromText('POINT (120 30)', 4326));
```
## spring data jpa查询空间数据

```java
import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.annotations.NaturalId;
import javax.persistence.*;
public class GeometryTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic( fetch = FetchType.LAZY )
    private Geometry geometry;

    // Getter、Setter方法省略
}

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.List;
public interface geoRepository extends JpaRepository<GeometryTable, Long> {
    /**
     * 按圆形缓冲区选择
     * @param center 缓冲区的中心点
     * @param radius 缓冲区半径
     */
    @Query("select g from GeometryTable as g where distance(g.geometry, :center) < :radius")
    List<GeometryTable> findAllByBuffer(@Param("center") Point center, @Param("radius") Double radius);

    @Query("select g from GeometryTable as g where within(g.geometry, :extent) = true")
    List<GeometryTable> findAllByExtent(@Param("extent") Polygon extent);

}
```

## JSON序列化geometry空间数据

如果使用的是jackson，那么只需要加入一个插件就行。

```java
import com.bedatadriven.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration 
public class Config{    
    @Bean
    public JtsModule jtsModule(){
        return new JtsModule();
    }
}
```

## mysql geometry数据使用注意事项
1. ST_GeomFromText('POINT (120 30)', 4326)语句中的4326是空间坐标系SRID(空间引用识别号)在同一张表里应该是最好存一种。如果在查询时使用不同的坐标系对象，会报SRID不一致的错误