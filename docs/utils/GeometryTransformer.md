# 地理空间数据坐标系的转换

## pom.xml依赖
```xml
    <!--参考:http://docs.geotools.org/latest/userguide/tutorial/quickstart/maven.html-->
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
    <properties>
        <geotools.version>20.1</geotools.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <java.version>1.8</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-api</artifactId>
            <version>${geotools.version}</version>
        </dependency>

        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-main</artifactId>
            <version>${geotools.version}</version>
        </dependency>

        <dependency>
            <groupId>org.geotools</groupId>
            <artifactId>gt-epsg-hsql</artifactId>
            <version>${geotools.version}</version>
        </dependency>
    </dependencies>

```
## java代码
```java

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.GeometryEditor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 坐标系查询<a href="http://epsg.io/">http://epsg.io/</a><br/>
 * Author: geshengbin
 * Email: kukukiki19940703@163.com
 */
public class GeoConverter {
    static {
        // Setting the system-wide default at startup time。设置GeoTools全局属性，定义参考坐标系的轴为x-y顺序
        System.setProperty("org.geotools.referencing.forceXY", "true");
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoConverter.class);
    private static double pi = 3.1415926535897932384626;
    private static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
    private static double a = 6378245.0;
    private static double ee = 0.00669342162296594323;
    private static class FactoryHolder{
        private static final GeometryFactory WGS_84_Pseudo_Mercator_F = new GeometryFactory(new PrecisionModel(), WGS_84_Pseudo_Mercator);
        private static final GeometryFactory GCS_WGS_1984_F = new GeometryFactory(new PrecisionModel(), GCS_WGS_1984);
        private static final GeometryFactory GCJ_02_F = new GeometryFactory(new PrecisionModel(), GCJ_02);
        private static final GeometryFactory BD_09_F = new GeometryFactory(new PrecisionModel(), BD_09);
    }

    // WGS 84 / Pseudo-Mercator -- Spherical Mercator, Google Maps, OpenStreetMap, Bing, ArcGIS, ESRI
    public static final int WGS_84_Pseudo_Mercator = 3857;
    // WGS 84 -- WGS84 - World Geodetic System 1984, used in GPS
    public static final int GCS_WGS_1984 = 4326;
    // 自定义GCJ02火星坐标系SRID
    public static final int GCJ_02 = -2;
    // 自定义百度09坐标系SRID
    public static final int BD_09 = -9;


    public static GeometryFactory getWGS_84_Pseudo_Mercator_Factory(){
        return GeoConverter.FactoryHolder.WGS_84_Pseudo_Mercator_F;
    }

    public static GeometryFactory getGCS_WGS_1984_Factory(){
        return GeoConverter.FactoryHolder.GCS_WGS_1984_F;
    }

    public static GeometryFactory getGCJ_02_Factory(){
        return GeoConverter.FactoryHolder.GCJ_02_F;
    }

    public static GeometryFactory getBD_09_Factory(){
        return GeoConverter.FactoryHolder.BD_09_F;
    }

    /**
     * 从其他坐标转为WGS 84 经纬度坐标
     * 支持两种国内的坐标：百度BD_09和火星坐标系GCJ_02。
     * @param geometry 待转换几何体
     * @return Geometry WGS84 经纬度坐标 2维坐标
     */
    public static Geometry toWGS84(Geometry geometry){
        return transform(geometry, GCS_WGS_1984);
    }

    /**
     * 从其他坐标转为WGS_84_Pseudo_Mercator 米制 笛卡尔坐标系
     * 支持两种国内的坐标：百度BD_09和火星坐标系GCJ_02。
     * @param geometry 待转换几何体
     * @return Geometry - WGS_84_Pseudo_Mercator笛卡尔坐标系下的几何体，只有x、y坐标，单位米
     */
    public static Geometry toWGS_84_Pseudo_Mercator(Geometry geometry){
        return transform(geometry, WGS_84_Pseudo_Mercator);
    }

    /**
     * 给定几何和目标坐标系的EPSG id，将几何体转换到目标坐标系。
     * 支持两种国内的坐标：百度BD_09和火星坐标系GCJ_02。
     * @param sourceGeometry 待转换几何体
     * @param targetSRID 目标坐标系id
     * @return Geometry 转换失败返回null
     */
    public static Geometry transform(Geometry sourceGeometry, int targetSRID){

        int sourceGeometrySRID = sourceGeometry.getSRID();
        if (targetSRID == sourceGeometrySRID){
            return sourceGeometry;
        }

        try {
            // GCJ_02 to BD_09
            if (sourceGeometrySRID == GCJ_02 && targetSRID == BD_09) {
                return gcj02_To_Bd09(sourceGeometry);
            }
            // BD_09 to GCJ_02
            if (sourceGeometrySRID == BD_09 && targetSRID == GCJ_02) {
                return bd09_To_Gcj02(sourceGeometry);
            }

            if (sourceGeometrySRID == GCJ_02) {
                Geometry geometryWgs84 = gcj02_To_Gps84(sourceGeometry);
                return transform(geometryWgs84, targetSRID);
            }

            if (sourceGeometrySRID == BD_09) {
                Geometry geometryWgs84 = bd09_To_gps84(sourceGeometry);
                return transform(geometryWgs84, targetSRID);
            }

            if (targetSRID == GCJ_02) {
                Geometry wgs84 = transform(sourceGeometry, GCS_WGS_1984);
                return gps84_To_Gcj02(wgs84);
            }

            if (targetSRID == BD_09) {
                Geometry wgs84 = transform(sourceGeometry, GCS_WGS_1984);
                return gps84_To_bd09(wgs84);
            }

            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:" + targetSRID);
            return transform(sourceGeometry, targetSRID, targetCRS);
        } catch (FactoryException e) {
            LOGGER.debug("EPSG factory not found", e);
        }
        return null;
    }

    private static Geometry transform(Geometry sourceGeometry, int targetSRID, CoordinateReferenceSystem targetCRS){
        try{
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:" + sourceGeometry.getSRID());

            // JTS.transform方法转换后geometry的SRID和GeometryFactory没有变化，所以这里先创建一个目标坐标系的GeometryFactory
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), targetSRID);
            sourceGeometry = geometryFactory.createGeometry(sourceGeometry);

            MathTransform mathTransform = CRS.findMathTransform(sourceCRS, targetCRS, false);
            Geometry transform = JTS.transform(sourceGeometry, mathTransform);

            return transform;
        }catch (Exception e){
            LOGGER.debug("转换失败", e);
        }
        return null;
    }

    /**
     * 84 to 火星坐标系 (GCJ-02) World Geodetic System ==> Mars Geodetic System<br/>
     * 只支持国内坐标转换
     * @param lat 纬度
     * @param lon 经度
     * @return {纬度, 经度}
     */
    public static double[] gps84_To_Gcj02(double lat, double lon) {
        if (outOfChina(lat, lon)) {
            return new double[]{lat,lon};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLat, mgLon};
    }

    private static Geometry gps84_To_Gcj02(Geometry wgs84){
        final GeometryFactory factory = getGCJ_02_Factory();
        GeometryEditor editor = new GeometryEditor(factory);
        return editor.edit(wgs84, new GeometryEditor.CoordinateSequenceOperation(){
            @Override
            public CoordinateSequence edit(CoordinateSequence coordinateSequence, Geometry geometry) {
                Coordinate[] sourceCoordinates = wgs84.getCoordinates();
                Coordinate[] coordinates = new Coordinate[sourceCoordinates.length];
                for (int i = 0; i < coordinates.length; i++) {
                    double[] doubles = gps84_To_Gcj02(sourceCoordinates[i].y, sourceCoordinates[i].x);
                    coordinates[i] = new Coordinate(doubles[1], doubles[0], sourceCoordinates[i].z);
                }
                return factory.getCoordinateSequenceFactory().create(coordinates);
            }
        });
    }

    /**
     * 火星坐标系 (GCJ-02) to 84
     * @param lat 纬度
     * @param lon 经度
     * @return {纬度, 经度}
     * */
    public static double[] gcj02_To_Gps84(double lat, double lon) {
        double[] gps = transform(lat, lon);
        double longitude = lon * 2 - gps[1];
        double latitude = lat * 2 - gps[0];
        return new double[]{latitude, longitude};
    }
    private static Geometry gcj02_To_Gps84(Geometry gcj02){
        GeometryFactory gcs_wgs_1984_factory = getGCS_WGS_1984_Factory();
        GeometryEditor editor = new GeometryEditor(gcs_wgs_1984_factory);
        return editor.edit(gcj02, new GeometryEditor.CoordinateSequenceOperation() {
            public CoordinateSequence edit(CoordinateSequence coordSeq, Geometry geometry) {

                Coordinate[] sourceCoordinates = gcj02.getCoordinates();
                Coordinate[] coordinates = new Coordinate[sourceCoordinates.length];
                for (int i = 0; i < coordinates.length; i++) {
                    double[] doubles = gcj02_To_Gps84(sourceCoordinates[i].y, sourceCoordinates[i].x);
                    coordinates[i] = new Coordinate(doubles[1], doubles[0], sourceCoordinates[i].z);
                }
                return gcs_wgs_1984_factory.getCoordinateSequenceFactory().create(coordinates);
            }
        });
    }

    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法.
     * 将 GCJ-02 坐标转换成 BD-09 坐标
     * @param lat 纬度
     * @param lon 经度
     * @return {纬度, 经度}
     */
    public static double[] gcj02_To_Bd09(double lat, double lon) {
        double x = lon, y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * x_pi);
        double tempLon = z * Math.cos(theta) + 0.0065;
        double tempLat = z * Math.sin(theta) + 0.006;
        double[] gps = {tempLat,tempLon};
        return gps;
    }
    private static Geometry gcj02_To_Bd09(Geometry gcj02){
        GeometryFactory bd_09_factory = getBD_09_Factory();
        GeometryEditor editor = new GeometryEditor(bd_09_factory);
        return editor.edit(gcj02, new GeometryEditor.CoordinateSequenceOperation() {
            public CoordinateSequence edit(CoordinateSequence coordSeq, Geometry geometry) {

                Coordinate[] sourceCoordinates = gcj02.getCoordinates();
                Coordinate[] coordinates = new Coordinate[sourceCoordinates.length];
                for (int i = 0; i < coordinates.length; i++) {
                    double[] doubles = gcj02_To_Bd09(sourceCoordinates[i].y, sourceCoordinates[i].x);
                    coordinates[i] = new Coordinate(doubles[1], doubles[0], sourceCoordinates[i].z);
                }
                return bd_09_factory.getCoordinateSequenceFactory().create(coordinates);
            }
        });
    }

    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法.
     * 将 BD-09 坐标转换成GCJ-02 坐标
     * @param lat 纬度
     * @param lon 经度
     * @return {纬度, 经度}
     */
    public static double[] bd09_To_Gcj02(double lat, double lon) {
        double x = lon - 0.0065, y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
        double tempLon = z * Math.cos(theta);
        double tempLat = z * Math.sin(theta);
        double[] gps = {tempLat,tempLon};
        return gps;
    }
    private static Geometry bd09_To_Gcj02(Geometry bd09){
        GeometryFactory factory = getGCJ_02_Factory();
        GeometryEditor editor = new GeometryEditor(factory);
        return editor.edit(bd09, new GeometryEditor.CoordinateSequenceOperation() {
            public CoordinateSequence edit(CoordinateSequence coordSeq, Geometry geometry) {

                Coordinate[] sourceCoordinates = bd09.getCoordinates();
                Coordinate[] coordinates = new Coordinate[sourceCoordinates.length];
                for (int i = 0; i < coordinates.length; i++) {
                    double[] doubles = bd09_To_Gcj02(sourceCoordinates[i].y, sourceCoordinates[i].x);
                    coordinates[i] = new Coordinate(doubles[1], doubles[0], sourceCoordinates[i].z);
                }
                return factory.getCoordinateSequenceFactory().create(coordinates);
            }
        });
    }

    /**
     * 将gps84转为bd09
     * @param lat
     * @param lon
     * @return
     */
    public static double[] gps84_To_bd09(double lat,double lon){
        double[] gcj02 = gps84_To_Gcj02(lat,lon);
        double[] bd09 = gcj02_To_Bd09(gcj02[0],gcj02[1]);
        return bd09;
    }
    private static Geometry gps84_To_bd09(Geometry wgs84){
        GeometryFactory factory = getBD_09_Factory();
        GeometryEditor editor = new GeometryEditor(factory);
        return editor.edit(wgs84, new GeometryEditor.CoordinateSequenceOperation() {
            public CoordinateSequence edit(CoordinateSequence coordSeq, Geometry geometry) {

                Coordinate[] sourceCoordinates = wgs84.getCoordinates();
                Coordinate[] coordinates = new Coordinate[sourceCoordinates.length];
                for (int i = 0; i < coordinates.length; i++) {
                    double[] doubles = gps84_To_bd09(sourceCoordinates[i].y, sourceCoordinates[i].x);
                    coordinates[i] = new Coordinate(doubles[1], doubles[0], sourceCoordinates[i].z);
                }
                return factory.getCoordinateSequenceFactory().create(coordinates);
            }
        });
    }

    /**
     * @param lat 纬度
     * @param lon 经度
     * @return {纬度, 经度}
     */
    public static double[] bd09_To_gps84(double lat,double lon){
        double[] gcj02 = bd09_To_Gcj02(lat, lon);
        double[] gps84 = gcj02_To_Gps84(gcj02[0], gcj02[1]);
        //保留小数点后六位
        gps84[0] = retain6(gps84[0]);
        gps84[1] = retain6(gps84[1]);
        return gps84;
    }
    private static Geometry bd09_To_gps84(Geometry bd09){
        GeometryFactory factory = getGCS_WGS_1984_Factory();
        GeometryEditor editor = new GeometryEditor(factory);
        return editor.edit(bd09, new GeometryEditor.CoordinateSequenceOperation() {
            public CoordinateSequence edit(CoordinateSequence coordSeq, Geometry geometry) {
                Coordinate[] sourceCoordinates = bd09.getCoordinates();
                Coordinate[] coordinates = new Coordinate[sourceCoordinates.length];
                for (int i = 0; i < coordinates.length; i++) {
                    double[] doubles = bd09_To_gps84(sourceCoordinates[i].y, sourceCoordinates[i].x);
                    coordinates[i] = new Coordinate(doubles[1], doubles[0], sourceCoordinates[i].z);
                }
                return factory.getCoordinateSequenceFactory().create(coordinates);
            }
        });
    }

    /**
     * 保留小数点后六位
     */
    private static double retain6(double num){
        String result = String .format("%.6f", num);
        return Double.valueOf(result);
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }
    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
                * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0
                * pi)) * 2.0 / 3.0;
        return ret;
    }
    private static double[] transform(double lat, double lon) {
        if (outOfChina(lat, lon)) {
            return new double[]{lat,lon};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLat,mgLon};
    }

    /**
     * 判断是否在国内，不在国内则不做偏移
     */
    private static boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        if (lat < 0.8293 || lat > 55.8271)
            return true;
        return false;
    }
}

```