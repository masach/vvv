package entity.OJ.JudgeSystem;

import entity.OJ.OTHOJ;
import entity.Problem;
import entity.RES;
import entity.Result;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections.comparators.BooleanComparator;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import util.HTML.problemHTML;
import util.Main;
import util.MyClient;
import util.Pair;
import util.Tool;
import util.Vjudge.VjSubmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by syimlzhu on 2017/1/17.
 */
public class JudgeSystem extends OTHOJ {
    private String url= Main.GV.getJSONObject("judge_system").getString("URL");
    private MyClient hc = MyClient.getMyClient();
    public RES res = new RES();
    public final Boolean lock = false;
    @Override
    public String getRid(String user, VjSubmitter s) {
        return s.rid;
    }

    @Override
    public problemHTML getProblemHTML(String pid) {
        problemHTML problemHTML = new problemHTML();
        problemHTML.setTimeLimit("1000MS");
        problemHTML.setMenoryLimit("128M");
        problemHTML.setInt64("%lld");
        return problemHTML;
    }

    @Override
    public String getTitle(String pid) {
        return "JudgeSystem不能获取题目";
    }

    @Override
    public String submit(VjSubmitter s) {
        Pair<Integer,Integer> limit=Main.problems.getProblemLimit(Integer.parseInt(s.getSubmitInfo().pid));
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("type","submit"));
        formparams.add(new BasicNameValuePair("pid",s.getSubmitInfo().pid));
        formparams.add(new BasicNameValuePair("rid",s.getSubmitInfo().rid+""));
        formparams.add(new BasicNameValuePair("timelimit",limit.getKey()+""));
        formparams.add(new BasicNameValuePair("memorylimit",limit.getValue()+""));
        formparams.add(new BasicNameValuePair("code",s.getSubmitInfo().code));
        formparams.add(new BasicNameValuePair("language","0"));
        synchronized (lock) {
            String ret = hc.Post(url+"/",formparams);
            s.rid = Math.random()+"";
            /*Tool.debug("wait begin");
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Tool.debug("wait end");
            }*/
        }
        //if(ret == null) return "error";
        //else {
        return "success";
        //}
    }
    private static Map<String,Result> resultmap = new HashMap<>();
    static{
        resultmap.put("AC",Result.AC);
        resultmap.put("TLE",Result.TLE);
        resultmap.put("MLE",Result.MLE);
        resultmap.put("RE",Result.RE);
        resultmap.put("OLE",Result.OLE);
        resultmap.put("WA",Result.WA);
        resultmap.put("PE",Result.PE);
    }
    public void setResult(String result){
        JSONObject resultJson = JSONObject.fromObject(result);
        if(resultJson.getString("type").equals("padding")){
            res.setR(Result.PENDING);
            res.setTime("-");
            res.setMemory("-");
        }else if(resultJson.getString("type").equals("judging")){
            res.setR(Result.JUDGING);
            res.setTime("-");
            res.setMemory("-");
        }else if(resultJson.getString("type").equals("CE")){
            res.setR(Result.CE);
            res.setCEInfo(resultJson.getString("info"));
            res.setTime("-");
            res.setMemory("-");
        }else {
            int time = 0;
            int memory = 0;
            Result result_1;
            JSONArray retJson = resultJson.getJSONArray("ret");
            result_1 = Result.ERROR;
            for (int i = 0; i < retJson.size(); i++) {
                result_1 = resultmap.get(retJson.getJSONArray(i).getString(1));
                time += retJson.getJSONArray(i).getInt(2);
                memory = Math.max(memory, retJson.getJSONArray(i).getInt(3));
                if (result_1 != Result.AC) {
                    break;
                }
            }
            res.setR(result_1);
            res.setTime(time + "MS");
            res.setMemory(memory + "KB");
        }
    }

    @Override
    public RES getResultReturn(VjSubmitter s){
        int wait[] = {1,1,1,2,3,5,7,8,9,10};
        int i=0;
        RES r = res;
        do{
            Tool.sleep(wait[i<wait.length?i:wait.length-1]*1000);
            i++;

            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("type","getResult"));
            formparams.add(new BasicNameValuePair("rid",s.getSubmitInfo().rid+""));
            String judge_system_ret = hc.Post(url+"/",formparams);
            Tool.debug(judge_system_ret);
            JSONObject jo = JSONObject.fromObject(judge_system_ret);
            if(jo.getString("ret").equals("success")){
                setResult(jo.getString("result"));
            }else if(jo.getString("ret").equals("noSubmit")){
                r.setR(Result.ERROR);
            }

            //JudgeSystem用https协议主动返回
            if(r.getR() == Result.AC) {
                Pair<Integer, Integer> limit = Main.problems.getProblemLimit(Integer.parseInt(s.getSubmitInfo().pid));
                if(r.getUseTime() > limit.getKey()){
                    r.setR(Result.TLE);
                }
            }
            if(r.getR() == Result.MLE || r.getR() == Result.OLE){
                r.setTime("-");
            }

            //System.out.println(submitterID+":get res="+r.getR());
            s.setShowstatus("评测结果="+r.getR());
            if(i>=30){
                Main.status.setStatusResult(s.getSubmitInfo().rid, Result.ERROR, "-", "-", "ERROR:评测超时。可能是原oj繁忙");
                break;
            }
        }while(!r.canReturn());
        return r;
    }

    @Override
    public RES getResult(VjSubmitter s) {
        return null;
    }

    @Override
    public String getProblemURL(String pid) {
        return "";
    }

    @Override
    public String getName() {
        return "JudgeSystem";
    }

    @Override
    public String get64IO(String pid) {
        return "%lld";
    }
}
