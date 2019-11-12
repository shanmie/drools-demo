#!/usr/bin/env bash
#删除模板
curl -X DELETE '192.168.6.22:9200/_template/test_template'

#修改模板 feild类型为keyword不分词,如果为text默认分词
curl -X PUT "192.168.6.22:9200/_template/test_template" -H 'Content-Type: application/json' -d'
{
	"index_patterns": ["test*"],
	"mappings": {
		"tag": {
			"dynamic": true,
			"properties": {
				"country": {
					"type": "text",
     				"analyzer": "ik_max_word",
     				"search_analyzer": "ik_max_word"
				}
			}
		}
	}
}
'
#删掉所有索引
curl -X DELETE '192.168.6.22:9200/*'

#查看当前索引分页大小
curl -X GET '192.168.6.22:9200/test/_settings'

#修改索引分页大小 如果设置所有 test 改成 _all
curl -X PUT '192.168.40.31:9200/test/_settings'-d '
{
    "index.max_result_window" :"1000000"
}
'

#查看该查询如何默认分词
curl -H "Content-Type: application/json" GET 'http://192.168.6.22:9200/_analyze?pretty=true' -d '
{
	"text":"出发口岸北京,常驻城市北京,有钱人,25"
}
'

#查看该查询如何使用中文分词 max 模式
curl -H "Content-Type: application/json" POST 'http://192.168.6.22:9200/_analyze?pretty=true' -d '
{
	"text": "出发口岸北京,常驻城市北京,有钱人,25",
  	"analyzer": "ik_max_word"
}
'

#查看该查询如何使用中文分词 smart 模式
curl -H "Content-Type: application/json" POST 'http://192.168.6.22:9200/_analyze?pretty=true' -d '
{
	"text": "出发口岸北京,常驻城市北京,有钱人,25",
  	"analyzer": "ik_smart"
}
'