using System;
using System.Collections.Generic;
using System.Text;
using TaskScheduler;
using System.Windows.Forms;

namespace win7bel_scheduler
{
    class Scheduler
    {
        static void Main()
        {
            String instDir = Environment.GetEnvironmentVariable("PROGRAMFILES") + "\\win7bel\\";
            try
            {
                TaskSchedulerClass ts = new TaskSchedulerClass();
                ts.Connect(null, null, null, null);
                ITaskFolder root = ts.GetFolder("\\");

                ITaskDefinition task = ts.NewTask(0);
            
                task.Settings.RunOnlyIfNetworkAvailable = true;
                task.Settings.StartWhenAvailable = true;

                IDailyTrigger trigger = (IDailyTrigger)task.Triggers.Create(_TASK_TRIGGER_TYPE2.TASK_TRIGGER_DAILY);
                trigger.StartBoundary = "2011-01-01T15:00:00";

                IExecAction exec = (IExecAction)task.Actions.Create(_TASK_ACTION_TYPE.TASK_ACTION_EXEC);
                exec.Id = "win7bel-updater";
                exec.Path = instDir + "win7bel-updater.exe";

                IRegisteredTask regTask = root.RegisterTaskDefinition("win7bel-updater", task, (int)_TASK_CREATION.TASK_CREATE_OR_UPDATE, null, null, _TASK_LOGON_TYPE.TASK_LOGON_INTERACTIVE_TOKEN, "");
            }
            catch (Exception ex)
            {
                MessageBox.Show( "Памылка ўсталёўкі спраўджвання абнаўленняў перакладу Windows 7\nПаведаміце, калі ласка, на i18n-bel-win7@googlegroups.com :\n"+ex.Message,"Памылка", MessageBoxButtons.OK, MessageBoxIcon.Error);
                Environment.Exit(1);
            }
            Environment.Exit(0);
        }
    }
}
