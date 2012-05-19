#include <ros/publisher.h>
#include <ros/subscriber.h>
#include <ros/node_handle.h>
#include <sensor_msgs/Image.h>
#include <cv_bridge/CvBridge.h>
#include <opencv/cv.h>
std::map<std::string, ros::Publisher> mPublishers;
ros::Subscriber imgSub;
sensor_msgs::CvBridge bridge;
template <class msg_type>
ros::Publisher getPublisher(const std::string& topic)
{

  if (mPublishers.find(topic) == mPublishers.end())
  {
    std::ostringstream ostr;
    ostr << "/" << "wide_stereo" << "/" << topic;
    ros::NodeHandle mNh;
    mPublishers[topic] = mNh.advertise<msg_type> (ostr.str(), 0, true);
  }

  return mPublishers[topic];
}

void cb(const sensor_msgs::ImageConstPtr& image_msg)
{
  IplImage* imgSrc = bridge.imgMsgToCv(image_msg,"passthrough");
  IplImage* imgDest = cvCreateImage(cvSize((int)(imgSrc->width * 0.5),(int)(imgSrc->height * 0.5) ),imgSrc->depth,imgSrc->nChannels);
  
  cvResize(imgSrc,imgDest,CV_INTER_LINEAR);
  
  ros::Publisher pub = getPublisher<sensor_msgs::Image>("resize");
  pub.publish(bridge.cvToImgMsg(imgDest,"bgr8"));
 
}


int main (int argc, char** argv)
{
    // Initialize ROS
  ros::init (argc, argv, "image_resize");

  ros::NodeHandle nh;

  imgSub = nh.subscribe("/wide_stereo/right/image_color", 1, cb);

  std::cout << "ready!" << std::endl;

    // Spin
  ros::spin ();

  return (0);
}
