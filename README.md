# S3-ImageResize
Auto Resize Image for S3 Storage

1.将本文档打包成jar包上传到aws的lambda中
2.在lambda中配置API GateWay，ApiGateWay指向此lambda，APIGateWay的URL在go代码中的apiGateWay中进行指定,ApiGateWay中配置路由/resize-image-lambda


```golang code for getWay
const S3Host = "Your S3 host"
const ApiGateWay = "Your APi GateWay/resize-image-lambda?key=%s&size=%s"

func AutoResizeImg(c *gin.Context){
	s3Uri := c.Query("uri")
	size := c.Query("size")
	//乘号兼容X x 和 *
	size = strings.Replace(strings.ToLower(size),"*","x",1)

	// 原图地址
	resourceUrl:= S3Host+s3Uri
	// 暂时不支持svg和gif
	if size=="" || !strings.Contains(size,"x") || strings.HasSuffix(s3Uri,"svg") || strings.HasSuffix(s3Uri,"gif"){
		c.Redirect(http.StatusMovedPermanently, resourceUrl)
		return
	}

	// 这里的算法必须和Lambda中的一样
	var resizeKey string
	dotIndex := strings.LastIndex(s3Uri,".")
	if dotIndex>0{
		resizeKey = s3Uri[0:dotIndex]+"_"+size+s3Uri[dotIndex:]
	}else{
		resizeKey = s3Uri+"_"+size
	}

	resizeUrl := S3Host+resizeKey
	resizeCacheKey := "ResourceCache:"+resizeUrl
	// 如果已经确定资源存在，直接重定向
	if existByte,err := redis.Get(resizeCacheKey);err==nil{
		if existByte[0]=='1'{
			c.Redirect(http.StatusMovedPermanently, resizeUrl)
		}else{
			c.Redirect(http.StatusMovedPermanently, resourceUrl)
		}
		return
	}
	// 用Head方法测试压缩后的文件是否已经存在
	resp,err := http.Head(resizeUrl)
	// 不存在，调用S3的Lambda进行文件压缩
	if err!=nil || resp.StatusCode!=200{
		lambdaUrl := fmt.Sprintf(ApiGateWay,s3Uri,size)
		resp,err = http.Get(lambdaUrl)
		if err!=nil{
			// 压缩失败，直接返回原图，异常情况，一般不会出现
			c.Redirect(http.StatusMovedPermanently, resourceUrl)
			return
		}
		body, _ := ioutil.ReadAll(resp.Body)
		var apiGateWayResp ApiGateWayResponse
		err = json.Unmarshal(body,&apiGateWayResp)
		if err!=nil{
			// 调用压缩接口失败，直接返回原图 ，异常情况，一般不会出现
			c.Redirect(http.StatusMovedPermanently, resourceUrl)
			return
		}
		if apiGateWayResp.Key!=""{
			if apiGateWayResp.Key==resizeKey{
				// 压缩成功
				redis.Set(resizeCacheKey, 0, []byte{'1'})
				c.Redirect(http.StatusMovedPermanently,resizeUrl)
			}else{
				// 压缩失败
				redis.Set(resizeCacheKey, 30*60, []byte{'0'})
				c.Redirect(http.StatusMovedPermanently,resourceUrl)
			}
			return
		}
	}else{
		// 如果已存在，直接缓存,并返回
		redis.Set(resizeCacheKey, 0, []byte{'1'})
		c.Redirect(http.StatusMovedPermanently,resizeUrl)
		return
	}
	c.Redirect(http.StatusMovedPermanently, resourceUrl)
}
```