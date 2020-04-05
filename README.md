# RSocket Proxy for [Syaberu](https://github.com/making/syaberu)

1. Download latest syaberu from [repository](https://oss.sonatype.org/content/repositories/snapshots/am/ik/lab/syaberu-server/0.0.1-SNAPSHOT/) 

1. Download latest syaberu-rsocket-proxy from [repository](https://oss.sonatype.org/content/repositories/snapshots/am/ik/lab/syaberu-rsocket-proxy/0.0.1-SNAPSHOT) 

```
java -jar syaberu-rsocket-proxy-0.0.1*.jar

java -jar syaberu-0.0.1*.jar --syaberu.proxy-uri=http://localhost:80802/rsocket --syaberu.proxy-subscription-id=foo 
```

```
curl localhost:8082/proxy/foo -H "X-Api-Key: ****" -d text=18:00になりました。帰宅の準備をしてください。 -d speaker=haruka -d emotion=happiness
```


## From syaberu-rsocket-proxy 0.1

Since 0.1, syaberu-rsocket-proxy uses MySQL for schedule calls.

```
brew install mysql
brew services start mysql
mysql -u root -e 'create database syaberu;'
```