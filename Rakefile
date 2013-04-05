def node_webkit_path
  return "/Applications/node-webkit.app/Contents/MacOS/node-webkit"
end

def node_running?
  return `pgrep node`.split("\n").size > 1
end

def node_pid
  return `pgrep node`.split("\n").first.to_i
end

def kill_node
  Process.kill('INT', node_pid()) if node_running?
end

def start_node
  run_node = `node app.js &`
end

def start_app
  run = `#{node_webkit_path} #{Dir.pwd} $@`
end

task :kill_node do
  kill_node
end

task :run do
  if node_running?
    puts "starting app"
    start_app
  else
    puts "starting node server"
    start_node
    puts "starting app"
    start_app
  end
end