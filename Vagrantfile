# -*- mode: ruby -*-
# vi: set ft=ruby :

$root_script = <<-SHELL
export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y pcscd pcsc-tools python3-dev python3-pyscard python3-pip 

apt-get -y install git
apt-get -y install openjdk-8-jre
apt-get -y install openjdk-8-jdk
apt-get -y install ant

pip3 install ipython
pip3 install jupyter
SHELL

$user_script = <<-SHELL
cd /vagrant  
mkdir Projects
cd Projects/
git clone https://github.com/martinpaljak/AppletPlayground.git
    
cd /vagrant/Projects/
mkdir -p GlobalPlatformPro
cd GlobalPlatformPro
wget https://github.com/martinpaljak/GlobalPlatformPro/releases/download/v20.01.23/gp.jar    
echo "alias gp=\'java -jar /vagrant/Projects/GlobalPlatformPro/gp.jar\'" > /home/vagrant/.bash_aliases

cd /vagrant/Projects/
cd AppletPlayground/
ant
    
cd /vagrant/Projects/
cp -r AppletPlayground/ext .
SHELL


Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/focal64"
  config.vm.network "forwarded_port", guest: 8888, host: 8888
  
  config.vm.provision "as-root",
                      type: "shell",
                      preserve_order: true,
                      inline: $root_script
  
  config.vm.provision "as-user",
                      type: "shell",
                      preserve_order: true,
                      privileged: false,
                      inline: $user_script

  config.vm.provider "virtualbox" do |v|
    v.customize ["modifyvm", :id, "--usb", "on"]
    v.customize ["modifyvm", :id, "--usbohci", "on"]
    v.customize ["usbfilter", "add", "0",
                 "--target", :id,
                 "--name", "Card Reader",
                 "--manufacturer", "Gemplus",
                 "--product", "USB SmartCard Reader"]
    v.customize ["usbfilter", "add", "1",
                 "--target", :id,
                 "--name", "Generic EMV Smartcard Reader [F0F0]",
                 "--manufacturer", "Generic",
                 "--product", "EMV Smartcard Reader"]
    v.customize ["usbfilter", "add", "2",
                 "--target", :id,
                 "--name", "Generic Smart Card Reader Interface [6123]",
                 "--manufacturer", "Generic",
                 "--product", "Generic Smart Card Reader Interface [6123]"]
  end

end

