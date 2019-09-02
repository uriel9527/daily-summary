package me.w1992wishes.spark.base.df

import org.apache.spark.sql.SparkSession

/**
  * @author w1992wishes 2019/7/10 10:52
  */
object MakeDF {

  def main(args: Array[String]): Unit = {

    val sparkSession = SparkSession.builder()
      .appName("RDD to DataFrame")
      .master("local")
      .getOrCreate()
    import sparkSession.implicits._

    val df = List(
      ("站点1", "2017-01-01", 50),
      ("站点1", "2017-01-02", 45),
      ("站点1", "2017-01-03", 55),
      ("站点2", "2017-01-01", 25),
      ("站点2", "2017-01-02", 29),
      ("站点2", "2017-01-03", 27)
    ).toDF("site", "date", "user_cnt")
    df.show()

  }

}